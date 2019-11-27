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

package software.amazon.disco.agent.plugin.source;

import software.amazon.disco.agent.event.Event;
import software.amazon.disco.agent.event.Listener;

import java.util.LinkedList;
import java.util.List;

public class PluginListener implements Listener {
    public List<Event> events = new LinkedList<>();

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void listen(Event e) {
        events.add(e);
    }
}
