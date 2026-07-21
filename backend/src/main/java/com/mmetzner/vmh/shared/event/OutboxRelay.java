package com.mmetzner.vmh.shared.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxRelayProperties properties;

    @Value("${app.kafka.topic.name:vehicle-maintenance-events.v1}")
    private String topicName;

    @Transactional
    public int publishPendingBatch() {
        List<OutboxEventEntity> events = repository.lockPendingBatch(
                properties.batchSize(),
                properties.maxAttempts()
        );

        for (OutboxEventEntity event : events) {
            publish(event);
        }

        return events.size();
    }

    private void publish(OutboxEventEntity event) {
        try {
            String value = objectMapper.writeValueAsString(event.getPayload());
            kafkaTemplate.send(topicName, event.getVehicleId().toString(), value)
                    .get(properties.publishTimeout().toMillis(), TimeUnit.MILLISECONDS);
            event.markPublished(OffsetDateTime.now(ZoneOffset.UTC));
            log.info("Outbox event published eventId={} eventType={} vehicleId={}",
                    event.getEventId(), event.getEventType(), event.getVehicleId());
        } catch (JsonProcessingException exception) {
            event.markFailed(exception.getMessage());
            log.error("Outbox serialization failed eventId={}", event.getEventId(), exception);
        } catch (Exception exception) {
            event.markFailed(exception.getMessage());
            log.warn("Outbox publication failed eventId={} attempt={}",
                    event.getEventId(), event.getAttempts(), exception);
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
