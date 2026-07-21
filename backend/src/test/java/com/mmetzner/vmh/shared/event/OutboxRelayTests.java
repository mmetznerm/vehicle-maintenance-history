package com.mmetzner.vmh.shared.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OutboxRelayTests {

    @Test
    void shouldPublishWithVehicleKeyAndMarkEventAfterAcknowledgement() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        UUID vehicleId = UUID.randomUUID();
        var envelope = objectMapper.createObjectNode().put("eventId", UUID.randomUUID().toString());
        OutboxEventEntity event = OutboxEventEntity.create(
                UUID.randomUUID(),
                EventType.VEHICLE_CREATED,
                vehicleId,
                vehicleId,
                envelope,
                OffsetDateTime.now()
        );
        when(repository.lockPendingBatch(10, 3)).thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        OutboxRelay relay = new OutboxRelay(
                repository,
                kafkaTemplate,
                objectMapper,
                new OutboxRelayProperties(10, 3, Duration.ofSeconds(1))
        );
        ReflectionTestUtils.setField(relay, "topicName", "vehicle-maintenance-events.v1");

        int published = relay.publishPendingBatch();

        assertThat(published).isEqualTo(1);
        assertThat(event.getPublishedAt()).isNotNull();
        verify(kafkaTemplate).send(
                eq("vehicle-maintenance-events.v1"),
                eq(vehicleId.toString()),
                anyString()
        );
    }
}
