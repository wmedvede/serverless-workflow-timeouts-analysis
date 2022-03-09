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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("external-service")
@ApplicationScoped
public class ExternalServiceResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalServiceResource.class);

    private static final String RESPONSE_EVENTS = "callback_response_events_out";

    private static final String VISA_DENIED_OUT = "visa_denied_out";

    private static final String VISA_APPROVED_OUT = "visa_approved_out";

    @Inject
    @Channel(RESPONSE_EVENTS)
    Emitter<String> eventsEmitter;

    @Inject
    @Channel(VISA_DENIED_OUT)
    Emitter<String> visaDeniedEmitter;

    @Inject
    @Channel(VISA_APPROVED_OUT)
    Emitter<String> visaApprovedEmitter;

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

    @Path("fireStateEventResponse")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response fireStateEventResponse(@QueryParam("processInstanceId") String processInstanceId, @QueryParam("eventType") String eventType, @QueryParam("emitter") String emitter) {
        LOGGER.debug("Firing state event response, for processInstanceId:{}, eventType: {}, emitter: {}", processInstanceId, eventType, emitter);
        String event = generateCloudEventForEventState(processInstanceId, eventType);
        if (VISA_APPROVED_OUT.equals(emitter)) {
            visaApprovedEmitter.send(event);
        } else if (VISA_DENIED_OUT.equals(emitter)) {
            visaDeniedEmitter.send(event);
        } else {
            throw new IllegalArgumentException("Emitter: " + emitter + " was not found");
        }
        return Response.ok("{}").build();
    }

    @Incoming("kogito_outgoing_stream_reader")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<Void> onEventEvent(Message<String> message) {
        LOGGER.info("Receive message: {}", message.getPayload());
        if (message.getPayload() != null && message.getPayload().contains("nack")) {
            LOGGER.debug("A nack message was received");
            return message.nack(new Exception("A nack message was received", new Exception("The internal cause!!")));
        } else {
            return CompletableFuture.runAsync(() -> handleEvent(message.getPayload()))
                    .thenRun(message::ack);
        }
    }

    private void handleEvent(String payload) {
        try {
            //do what you want;
        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
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

    public String generateCloudEventForEventState(String processInstanceId, String eventType) {
        try {
            return objectMapper.writeValueAsString(CloudEventBuilder.v1()
                                                           .withId(UUID.randomUUID().toString())
                                                           .withSource(URI.create(""))
                                                           .withType(eventType)
                                                           .withTime(OffsetDateTime.now())
                                                           .withExtension("kogitoprocrefid", processInstanceId)
                                                           .withData(JsonCloudEventData.wrap(objectMapper.createObjectNode()))
                                                           .build());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}