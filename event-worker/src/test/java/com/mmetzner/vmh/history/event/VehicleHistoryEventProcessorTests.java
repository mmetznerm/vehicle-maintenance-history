package com.mmetzner.vmh.history.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mmetzner.vmh.history.projection.MaintenanceHistoryRepository;
import com.mmetzner.vmh.history.projection.ProcessedEventEntity;
import com.mmetzner.vmh.history.projection.ProcessedEventRepository;
import com.mmetzner.vmh.history.projection.VehicleHistoryEntity;
import com.mmetzner.vmh.history.projection.VehicleHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleHistoryEventProcessorTests {

    private VehicleHistoryRepository vehicleRepository;
    private MaintenanceHistoryRepository maintenanceRepository;
    private ProcessedEventRepository processedEventRepository;
    private VehicleHistoryEventProcessor processor;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleHistoryRepository.class);
        maintenanceRepository = mock(MaintenanceHistoryRepository.class);
        processedEventRepository = mock(ProcessedEventRepository.class);
        processor = new VehicleHistoryEventProcessor(
                new ObjectMapper().findAndRegisterModules(),
                vehicleRepository,
                maintenanceRepository,
                processedEventRepository
        );
        ReflectionTestUtils.setField(processor, "consumerName", "vehicle-public-history-v1");
    }

    @Test
    void shouldApplyVehicleEventAndRecordIdempotency() {
        UUID eventId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());

        processor.process(vehicleEvent(eventId, vehicleId));

        ArgumentCaptor<VehicleHistoryEntity> vehicleCaptor =
                ArgumentCaptor.forClass(VehicleHistoryEntity.class);
        verify(vehicleRepository).save(vehicleCaptor.capture());
        assertThat(vehicleCaptor.getValue().getBrand()).isEqualTo("Honda");
        assertThat(vehicleCaptor.getValue().getModel()).isEqualTo("Civic");
        assertThat(vehicleCaptor.getValue().getManufactureYear()).isEqualTo(2020);
        verify(processedEventRepository).save(any(ProcessedEventEntity.class));
    }

    @Test
    void shouldIgnoreDuplicateEventWithoutMutatingProjection() {
        UUID eventId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        when(processedEventRepository.existsById(any())).thenReturn(true);

        processor.process(vehicleEvent(eventId, vehicleId));

        verifyNoInteractions(vehicleRepository, maintenanceRepository);
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldInvalidateProjectionWhenVehicleIsDeleted() {
        UUID eventId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        String event = """
                {
                  "eventId":"%s",
                  "eventType":"VehicleDeleted",
                  "eventVersion":1,
                  "aggregateId":"%s",
                  "vehicleId":"%s",
                  "occurredAt":"2026-07-21T18:00:00Z",
                  "correlationId":"request-delete",
                  "payload":{}
                }
                """.formatted(eventId, vehicleId, vehicleId);

        processor.process(event);

        verify(vehicleRepository).deleteById(vehicleId);
    }

    private String vehicleEvent(UUID eventId, UUID vehicleId) {
        return """
                {
                  "eventId":"%s",
                  "eventType":"VehicleCreated",
                  "eventVersion":1,
                  "aggregateId":"%s",
                  "vehicleId":"%s",
                  "occurredAt":"2026-07-21T18:00:00Z",
                  "correlationId":"request-123",
                  "payload":{
                    "brand":"Honda",
                    "model":"Civic",
                    "manufactureYear":2020,
                    "color":"Black"
                  }
                }
                """.formatted(eventId, vehicleId, vehicleId);
    }
}
