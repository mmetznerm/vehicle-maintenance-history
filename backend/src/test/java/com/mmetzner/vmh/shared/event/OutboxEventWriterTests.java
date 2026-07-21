package com.mmetzner.vmh.shared.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import java.util.UUID;

import static com.mmetzner.vmh.shared.infrastructure.web.RequestIdFilter.REQUEST_ID_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OutboxEventWriterTests {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void shouldCreateVersionedEnvelopeWithCorrelationId() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        OutboxEventWriter writer = new OutboxEventWriter(
                repository,
                new ObjectMapper().findAndRegisterModules()
        );
        UUID vehicleId = UUID.randomUUID();
        MDC.put(REQUEST_ID_MDC_KEY, "request-123");

        UUID eventId = writer.write(
                EventType.VEHICLE_CREATED,
                vehicleId,
                vehicleId,
                new VehicleEventPayload("Honda", "Civic", 2020, null)
        );

        ArgumentCaptor<OutboxEventEntity> captor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(repository).save(captor.capture());
        var event = captor.getValue();

        assertThat(event.getEventId()).isEqualTo(eventId);
        assertThat(event.getEventType()).isEqualTo("VehicleCreated");
        assertThat(event.getVehicleId()).isEqualTo(vehicleId);
        assertThat(event.getPayload().path("eventVersion").asInt()).isEqualTo(1);
        assertThat(event.getPayload().path("correlationId").asText()).isEqualTo("request-123");
        assertThat(event.getPayload().path("payload").has("plate")).isFalse();
        assertThat(event.getPayload().path("payload").has("ownerId")).isFalse();
    }
}
