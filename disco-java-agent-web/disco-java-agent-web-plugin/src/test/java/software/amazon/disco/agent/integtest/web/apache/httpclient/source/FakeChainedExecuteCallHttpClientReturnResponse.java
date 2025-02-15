/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.disco.agent.integtest.web.apache.httpclient.source;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHttpResponse;

public class FakeChainedExecuteCallHttpClientReturnResponse extends FakeChainedExecuteCallHttpClientBase {
    public HttpResponse fakeResponse = new BasicHttpResponse(new ProtocolVersion("protocol", 1, 1), 200, "");

    public FakeChainedExecuteCallHttpClientReturnResponse() {
        fakeResponse.setEntity(new BasicHttpEntity());
    }
    /**
     * {@inheritDoc}
     */
    @Override
    HttpResponse actualExecute() {
        return fakeResponse;
    }
}
