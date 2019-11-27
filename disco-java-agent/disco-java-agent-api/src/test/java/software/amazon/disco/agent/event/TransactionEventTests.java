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

package software.amazon.disco.agent.event;

import org.junit.Assert;
import org.junit.Test;

public class TransactionEventTests {
    @Test
    public void testTransactionBeginEvent() {
        TransactionEvent event = new TransactionBeginEvent("Origin");
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals(TransactionEvent.Operation.BEGIN, event.getOperation());
    }

    @Test
    public void testTransactionEndEvent() {
        TransactionEvent event = new TransactionEndEvent("Origin");
        Assert.assertEquals("Origin", event.getOrigin());
        Assert.assertEquals(TransactionEvent.Operation.END, event.getOperation());
    }
}
