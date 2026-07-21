package com.mmetzner.vmh.inconsistency.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mmetzner.vmh.inconsistency.infrastructure.MaintenanceInconsistencyEntity;
import com.mmetzner.vmh.inconsistency.infrastructure.MaintenanceInconsistencyRepository;
import com.mmetzner.vmh.inconsistency.infrastructure.ProcessedAlertEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MaintenanceInconsistencyAlertProcessorTests {

    private MaintenanceInconsistencyRepository inconsistencyRepository;
    private ProcessedAlertEventRepository processedRepository;
    private MaintenanceInconsistencyAlertProcessor processor;

    @BeforeEach
    void setUp() {
        inconsistencyRepository = mock(MaintenanceInconsistencyRepository.class);
        processedRepository = mock(ProcessedAlertEventRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        processor = new MaintenanceInconsistencyAlertProcessor(
                objectMapper, inconsistencyRepository, processedRepository
        );
        ReflectionTestUtils.setField(processor, "consumerName", "backend-alerts-v1");
    }

    @Test
    void shouldProjectDetectedAndResolvedEvents() {
        UUID alertId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        OffsetDateTime detectedAt = OffsetDateTime.parse("2026-07-20T12:00:00Z");
        when(inconsistencyRepository.findById(alertId)).thenReturn(Optional.empty());

        processor.process(event(UUID.randomUUID(), "MaintenanceInconsistencyDetected", alertId, vehicleId, detectedAt));

        var entityCaptor = org.mockito.ArgumentCaptor.forClass(MaintenanceInconsistencyEntity.class);
        verify(inconsistencyRepository).save(entityCaptor.capture());
        MaintenanceInconsistencyEntity projected = entityCaptor.getValue();
        assertThat(projected.getStatus()).isEqualTo("ACTIVE");
        assertThat(projected.getRule()).isEqualTo("ODOMETER_ROLLBACK");

        reset(inconsistencyRepository);
        when(inconsistencyRepository.findById(alertId)).thenReturn(Optional.of(projected));
        processor.process(event(
                UUID.randomUUID(), "MaintenanceInconsistencyResolved", alertId, vehicleId, detectedAt.plusMinutes(1)
        ));

        assertThat(projected.getStatus()).isEqualTo("RESOLVED");
        assertThat(projected.getResolvedAt()).isEqualTo(detectedAt.plusMinutes(1));
        verify(processedRepository, times(2)).save(any());
    }

    @Test
    void shouldIgnoreAlreadyProcessedEvents() {
        when(processedRepository.existsById(any())).thenReturn(true);

        processor.process(event(
                UUID.randomUUID(), "MaintenanceInconsistencyDetected", UUID.randomUUID(),
                UUID.randomUUID(), OffsetDateTime.now()
        ));

        verifyNoInteractions(inconsistencyRepository);
        verify(processedRepository, never()).save(any());
    }

    private String event(UUID eventId, String eventType, UUID alertId, UUID vehicleId, OffsetDateTime occurredAt) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "eventVersion": 1,
                  "alertId": "%s",
                  "vehicleId": "%s",
                  "occurredAt": "%s",
                  "correlationId": "correlation-id",
                  "payload": {
                    "rule": "ODOMETER_ROLLBACK",
                    "severity": "CRITICAL",
                    "maintenanceIds": ["%s"],
                    "summary": "Odometer decreased",
                    "details": "Reading changed from 50000 km to 40000 km."
                  }
                }
                """.formatted(eventId, eventType, alertId, vehicleId, occurredAt, UUID.randomUUID());
    }
}
