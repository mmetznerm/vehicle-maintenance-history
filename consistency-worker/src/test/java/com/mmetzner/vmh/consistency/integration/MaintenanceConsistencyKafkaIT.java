package com.mmetzner.vmh.consistency.integration;

import com.mmetzner.vmh.consistency.outbox.AlertOutboxRepository;
import com.mmetzner.vmh.consistency.projection.ActiveInconsistencyRepository;
import com.mmetzner.vmh.consistency.projection.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class MaintenanceConsistencyKafkaIT {

    private static final String TOPIC = "vehicle-maintenance-consistency-it";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("maintenance-consistency")
            .withUsername("maintenance-consistency")
            .withPassword("maintenance-consistency");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.9.1")
    );

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ActiveInconsistencyRepository activeRepository;

    @Autowired
    private AlertOutboxRepository outboxRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.consumer-group", () -> "maintenance-consistency-it");
        registry.add("app.kafka.input-topic", () -> TOPIC);
        registry.add("app.kafka.output-topic", () -> "maintenance-alerts-it");
        registry.add("app.kafka.outbox.enabled", () -> "false");
    }

    @Test
    void shouldDetectResolveAndDeduplicateEventsConsumedFromKafka() {
        UUID vehicleId = UUID.randomUUID();
        UUID firstMaintenanceId = UUID.randomUUID();
        UUID secondMaintenanceId = UUID.randomUUID();
        String key = vehicleId.toString();

        send(key, envelope("VehicleCreated", UUID.randomUUID(), vehicleId, vehicleId,
                "2026-07-21T18:00:00Z", "{\"manufactureYear\":2020}"));
        send(key, envelope("MaintenanceCreated", UUID.randomUUID(), firstMaintenanceId, vehicleId,
                "2026-07-21T18:01:00Z",
                "{\"maintenanceDate\":\"2025-01-10\",\"odometer\":50000,\"description\":\"Oil change\"}"));
        UUID rollbackEventId = UUID.randomUUID();
        String rollbackEvent = envelope("MaintenanceCreated", rollbackEventId, secondMaintenanceId, vehicleId,
                "2026-07-21T18:02:00Z",
                "{\"maintenanceDate\":\"2025-02-10\",\"odometer\":40000,\"description\":\"Brake service\"}");
        send(key, rollbackEvent);
        send(key, rollbackEvent);

        await().ignoreExceptions().untilAsserted(() -> {
            assertThat(activeRepository.findAllByVehicleId(vehicleId)).singleElement()
                    .satisfies(alert -> assertThat(alert.getRule()).isEqualTo("ODOMETER_ROLLBACK"));
            assertThat(outboxRepository.count()).isEqualTo(1);
            assertThat(processedEventRepository.count()).isEqualTo(3);
        });

        send(key, envelope("MaintenanceUpdated", UUID.randomUUID(), secondMaintenanceId, vehicleId,
                "2026-07-21T18:03:00Z",
                "{\"maintenanceDate\":\"2025-02-10\",\"odometer\":60000,\"description\":\"Brake service\"}"));

        await().ignoreExceptions().untilAsserted(() -> {
            assertThat(activeRepository.findAllByVehicleId(vehicleId)).isEmpty();
            assertThat(outboxRepository.count()).isEqualTo(2);
            assertThat(outboxRepository.findAll()).extracting("eventType")
                    .containsExactlyInAnyOrder(
                            "MaintenanceInconsistencyDetected",
                            "MaintenanceInconsistencyResolved"
                    );
        });
    }

    private void send(String key, String value) {
        kafkaTemplate.send(TOPIC, key, value).join();
    }

    private String envelope(
            String eventType,
            UUID eventId,
            UUID aggregateId,
            UUID vehicleId,
            String occurredAt,
            String payload
    ) {
        return """
                {
                  "eventId":"%s",
                  "eventType":"%s",
                  "eventVersion":1,
                  "aggregateId":"%s",
                  "vehicleId":"%s",
                  "occurredAt":"%s",
                  "correlationId":"integration-test",
                  "payload":%s
                }
                """.formatted(eventId, eventType, aggregateId, vehicleId, occurredAt, payload);
    }
}
