/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.instrumentation.preprocess.loaders.classfiles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.disco.agent.inject.Injector;
import software.amazon.disco.instrumentation.preprocess.cli.PreprocessConfig;
import software.amazon.disco.instrumentation.preprocess.export.ExportStrategy;
import software.amazon.disco.instrumentation.preprocess.export.JarExportStrategy;
import software.amazon.disco.instrumentation.preprocess.util.JarFileUtils;
import software.amazon.disco.instrumentation.preprocess.util.PreprocessConstants;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A {@link ClassFileLoader} that loads all Jar files under specified paths
 */
public class JarLoader implements ClassFileLoader {
    private static final Logger log = LogManager.getLogger(JarLoader.class);
    private static Instrumentation instrumentation = Injector.createInstrumentation();

    /**
     * {@inheritDoc}
     */
    @Override
    public SourceInfo load(final Path path, final PreprocessConfig config) {
        log.debug(PreprocessConstants.MESSAGE_PREFIX + "Loading Jar: " + path);

        final SourceInfo info = loadJar(path.toFile(), new JarExportStrategy());

        if (info != null) {
            log.debug(PreprocessConstants.MESSAGE_PREFIX + "Jar loaded: " + path);
        }

        return info;
    }

    /**
     * Helper method to load one single Jar file
     *
     * @param file     Jar file to be loaded
     * @param strategy strategy used for exporting/saving the output artifact
     * @return {@link SourceInfo object} containing package data, null if file is not a valid {@link JarFile}
     */
    protected SourceInfo loadJar(final File file, final ExportStrategy strategy) {
        try (JarFile jarFile = new JarFile(file)) {
            final Map<String, byte[]> classFileData = new HashMap<>();

            if (jarFile == null) {
                log.warn(PreprocessConstants.MESSAGE_PREFIX + "Failed to load: " + file.getAbsolutePath());
                return null;
            }

            injectFileToSystemClassPath(file);

            log.debug(PreprocessConstants.MESSAGE_PREFIX + "Extracting class files from: " + file.getName());
            for (JarEntry entry : extractEntries(jarFile)) {
                if (entry.getName().endsWith(".class")) {
                    final String nameWithoutExtension = entry.getName().substring(0, entry.getName().lastIndexOf(".class")).replace('/', '.');

                    classFileData.put(nameWithoutExtension, JarFileUtils.readEntryFromJar(jarFile, entry));
                }
            }
            log.debug(PreprocessConstants.MESSAGE_PREFIX + "Class files extracted: " + classFileData.size());

            return classFileData.isEmpty() ? null : new SourceInfo(file, strategy, classFileData);
        } catch (Throwable t) {
            log.warn(PreprocessConstants.MESSAGE_PREFIX + "Invalid file, skipped", t);
            return null;
        }
    }

    /**
     * Helper method that iterates and extracts {@link JarEntry entries} that are class files
     *
     * @param jarFile Jar to explore
     * @return a list of {@link JarEntry entries} that are class files
     */
    protected List<JarEntry> extractEntries(final JarFile jarFile) {
        final List<JarEntry> result = new ArrayList<>();

        if (jarFile != null) {
            final Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();

                if (!e.isDirectory() && e.getName().endsWith(".class")) {
                    result.add(e);
                }
            }
        }
        return result;
    }

    /**
     * Add the file to the system class path using the {@link Injector injector} api.
     *
     * @param file Jar containing a set of classes to be added to the class path
     */
    protected void injectFileToSystemClassPath(final File file) {
        log.debug(PreprocessConstants.MESSAGE_PREFIX + "Injecting file to system class path: " + file.getName());
        Injector.addToSystemClasspath(instrumentation, file);
    }
}
