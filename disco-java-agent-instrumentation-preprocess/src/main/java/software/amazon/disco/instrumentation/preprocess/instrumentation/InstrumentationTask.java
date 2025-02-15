/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package software.amazon.disco.instrumentation.preprocess.instrumentation;

import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.disco.agent.plugin.ResourcesClassInjector;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.exceptions.InstrumentationException;
import software.amazon.disco.instrumentation.preprocess.export.ExportStrategy;
import software.amazon.disco.instrumentation.preprocess.instrumentation.InstrumentationOutcome.InstrumentationOutcomeBuilder;
import software.amazon.disco.instrumentation.preprocess.loaders.agents.TransformerExtractor;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.ClassFileLoader;
import software.amazon.disco.instrumentation.preprocess.loaders.classfiles.SourceInfo;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A task representing the overall work to be performed in order to process a single source, including loading the source, instrumenting
 * classes discovered from that source and ultimately saving the transformations done to a physical location.
 */
@AllArgsConstructor
public class InstrumentationTask {
    private static final Logger log = LogManager.getLogger(InstrumentationTask.class);

    private final ClassFileLoader loader;
    private final Path sourcePath;
    private final PreprocessConfig config;
    private final String relativeOutputPath;
    private final List<String> warnings = new ArrayList<>();

    /**
     * Triggers instrumentation of classes by invoking {@link ClassFileTransformer#transform(ClassLoader, String, Class, ProtectionDomain, byte[])} of
     * all ClassFileTransformers extracted via a {@link TransformerExtractor} and saves the transformed byte code according to the provided {@link ExportStrategy export strategy}
     * on a local file.
     *
     * @return outcome of the task. See {@link InstrumentationOutcome.Status}.
     */
    protected InstrumentationOutcome applyInstrumentation() {
        final SourceInfo sourceInfo = loader.load(sourcePath, config);

        if (sourceInfo != null) {
            log.debug(PreprocessConstants.MESSAGE_PREFIX + "Applying transformation on: " + sourceInfo.getSourceFile().getAbsolutePath());
            log.debug(PreprocessConstants.MESSAGE_PREFIX + "Classes found: " + sourceInfo.getClassByteCodeMap().size());

            for (Map.Entry<String, byte[]> entry : sourceInfo.getClassByteCodeMap().entrySet()) {
                applyInstrumentationOnClass(entry.getKey(), entry.getValue());
            }

            log.debug(PreprocessConstants.MESSAGE_PREFIX + getInstrumentationArtifacts().size() + " classes transformed");

            // invoke the configured export strategy to save transformed classes to a file
            sourceInfo.getExportStrategy().export(sourceInfo, getInstrumentationArtifacts(), config, relativeOutputPath);
        }

        final InstrumentationOutcomeBuilder builder = InstrumentationOutcome.builder().source(sourcePath.toString());

        // return the instrumentation outcome to be logged as summary
        if (!warnings.isEmpty()) {
            builder.status(InstrumentationOutcome.Status.WARNING_OCCURRED).failedClasses(warnings);
        } else {
            builder.status(getInstrumentationArtifacts().isEmpty() ? InstrumentationOutcome.Status.NO_OP : InstrumentationOutcome.Status.COMPLETED);
        }

        clearInstrumentationArtifacts();
        return builder.build();
    }

    /**
     * apply static instrumentation on a single class using all discovered {@link ClassFileTransformer}
     *
     * @param classFileName name of the class file
     * @param bytecode      raw bytecode of the class to be processed
     */
    protected void applyInstrumentationOnClass(final String classFileName, final byte[] bytecode) {
        try {
            // remove the classes. prefix for entries extracted from java.base.jmod file for easier handling such as
            // checking if a class has been transformed by another Installable already.
            final String nameWithoutPrefix = classFileName.startsWith("classes.") ? classFileName.substring(8) : classFileName;
            final String internalName = nameWithoutPrefix.replace('.', '/');

            for (ClassFileTransformer transformer : TransformerExtractor.getTransformers()) {
                final byte[] bytecodeToTransform = getInstrumentationArtifacts().containsKey(internalName) ?
                    getInstrumentationArtifacts().get(internalName).getClassBytes() : bytecode;

                transformer.transform(ClassLoader.getSystemClassLoader(), nameWithoutPrefix, null, null, bytecodeToTransform);
            }
        } catch (IllegalClassFormatException e) {
            throw new InstrumentationException("Failed to instrument : " + classFileName, e);
        } catch (InstrumentationException e) {
            // log this particular exception and skip to the next class when ByteBuddy fails to resolve certain dependency class during static instrumentation
            // if the "--failOnUnresolvableDependency" flag was not specified.
            if (e.getCause() != null && e.getCause() instanceof IllegalStateException && !config.isFailOnUnresolvableDependency()) {
                log.warn(PreprocessConstants.MESSAGE_PREFIX + "Failed to resolve dependency when instrumenting : " + classFileName, e);
                warnings.add(classFileName);
            } else {
                throw e;
            }
        }
    }

    /**
     * Fetches instrumented classes from the listener attached to all {@link software.amazon.disco.agent.interception.Installable installables}.
     *
     * @return a Map of class name as key and {@link InstrumentationArtifact} as value
     */
    protected Map<String, InstrumentationArtifact> getInstrumentationArtifacts() {
        final Map<String, InstrumentationArtifact> mergedMap = new HashMap<>();

        mergedMap.putAll(TransformationListener.getInstrumentedTypes());

        mergedMap.putAll(ResourcesClassInjector.getInjectedDependencies()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new InstrumentationArtifact(e.getValue()))
            ));

        return mergedMap;
    }

    /**
     * Empty the list of instrumented classes and dependency classes injected by {@link ResourcesClassInjector} in
     * preparation for transforming another package.
     */
    protected void clearInstrumentationArtifacts() {
        TransformationListener.getInstrumentedTypes().clear();
        ResourcesClassInjector.getInjectedDependencies().clear();
    }
}
