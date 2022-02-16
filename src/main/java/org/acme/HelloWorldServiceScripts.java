/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.acme;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.kie.kogito.internal.process.runtime.KogitoProcessContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class containing methods executed from the serverless workflow scripts.
 */
public class HelloWorldServiceScripts {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldServiceScripts.class);

    private HelloWorldServiceScripts() {
    }

    public static void initializationScript(KogitoProcessContext kcontext) {
        ObjectNode workflowData = (ObjectNode) kcontext.getVariable("workflowdata");
        workflowData.put("processInstanceId", kcontext.getProcessInstance().getStringId());
    }

    public static void printWorkflowData(String messsage, KogitoProcessContext kcontext) {
        ObjectNode workflowData = (ObjectNode) kcontext.getVariable("workflowdata");
        LOGGER.debug("{}, processInstanceId: {}, workflowdata: {}", messsage, kcontext.getProcessInstance().getStringId(), workflowData);
    }
}
