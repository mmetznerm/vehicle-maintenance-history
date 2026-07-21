package com.mmetzner.vmh.history.integration;

import com.mmetzner.vmh.history.api.PublicVehicleHistoryService;
import com.mmetzner.vmh.history.projection.MaintenanceHistoryRepository;
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
class PublicHistoryKafkaIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vehicle-history")
            .withUsername("vehicle-history")
            .withPassword("vehicle-history");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.9.1")
    );

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private PublicVehicleHistoryService publicHistoryService;

    @Autowired
    private MaintenanceHistoryRepository maintenanceRepository;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("app.kafka.consumer-group", () -> "vehicle-public-history-it");
        registry.add("app.kafka.topic", () -> "vehicle-maintenance-events-it");
    }

    @Test
    void shouldBuildIdempotentPublicProjectionFromKafkaEvents() {
        UUID vehicleId = UUID.randomUUID();
        UUID maintenanceId = UUID.randomUUID();
        UUID publicId = UUID.randomUUID();
        String key = vehicleId.toString();

        send(key, envelope("VehicleCreated", UUID.randomUUID(), vehicleId, vehicleId, """
                {"brand":"Honda","model":"Civic","manufactureYear":2020,"color":"Black"}
                """));
        send(key, envelope("MaintenanceCreated", UUID.randomUUID(), maintenanceId, vehicleId, """
                {"maintenanceDate":"2026-07-21","odometer":45000,"description":"Oil change","cost":350.00}
                """));
        String sharingEvent = envelope("VehicleHistorySharingChanged", UUID.randomUUID(), vehicleId, vehicleId,
                "{\"enabled\":true,\"publicId\":\"" + publicId + "\"}");
        send(key, sharingEvent);
        send(key, sharingEvent);

        await().ignoreExceptions().untilAsserted(() -> {
            var response = publicHistoryService.findByPublicId(publicId);
            assertThat(response.brand()).isEqualTo("Honda");
            assertThat(response.maintenances()).singleElement()
                    .satisfies(item -> assertThat(item.description()).isEqualTo("Oil change"));
            assertThat(maintenanceRepository.count()).isEqualTo(1);
        });
    }

    private void send(String key, String value) {
        kafkaTemplate.send("vehicle-maintenance-events-it", key, value).join();
    }

    private String envelope(
            String eventType,
            UUID eventId,
            UUID aggregateId,
            UUID vehicleId,
            String payload
    ) {
        return """
                {
                  "eventId":"%s",
                  "eventType":"%s",
                  "eventVersion":1,
                  "aggregateId":"%s",
                  "vehicleId":"%s",
                  "occurredAt":"2026-07-21T18:00:00Z",
                  "correlationId":"integration-test",
                  "payload":%s
                }
                """.formatted(eventId, eventType, aggregateId, vehicleId, payload.strip());
    }
}
