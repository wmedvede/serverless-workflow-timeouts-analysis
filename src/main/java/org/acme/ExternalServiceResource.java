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

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("external-service")
@ApplicationScoped
public class ExternalServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalServiceResource.class);

    private static final String RESPONSE_EVENTS = "callback_response_events_out";

    @Inject
    @Channel(RESPONSE_EVENTS)
    Emitter<String> eventsEmitter;

    @Inject
    ObjectMapper objectMapper;

    @Path("sendRequest")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendRequest(CallbackRequest request) {
        LOGGER.debug("CallbackRequest request received: {}", request);
        return Response.ok("{}").build();
    }

    @Path("resolveRequest")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response resolveRequest(ResolveCallbackRequest request) {
        String event = generateCloudEvent(request.getProcessInstanceId(), request.getQueryResponse());
        LOGGER.debug("Resolving request for processInstanceId:{}, event to send is: {}", request.getProcessInstanceId(), event);
        eventsEmitter.send(event);
        return Response.ok("{}").build();
    }

    public String generateCloudEvent(String processInstanceId, String queryResponse) {
        try {
            return objectMapper.writeValueAsString(CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create(""))
                    .withType("callback_response_events_in")
                    .withTime(OffsetDateTime.now())
                    .withExtension("kogitoprocrefid", processInstanceId)
                    .withData(JsonCloudEventData.wrap(objectMapper.createObjectNode().put("answer", queryResponse)))
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}