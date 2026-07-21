package com.mmetzner.vmh.consistency.outbox;

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
public class AlertOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(AlertOutboxRelay.class);

    private final AlertOutboxRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final AlertOutboxProperties properties;

    @Value("${app.kafka.output-topic}")
    private String topic;

    @Transactional
    public int publishPendingBatch() {
        List<AlertOutboxEntity> events = repository.lockPendingBatch(
                properties.batchSize(), properties.maxAttempts()
        );
        events.forEach(this::publish);
        return events.size();
    }

    private void publish(AlertOutboxEntity event) {
        try {
            String value = objectMapper.writeValueAsString(event.getPayload());
            kafkaTemplate.send(topic, event.getVehicleId().toString(), value)
                    .get(properties.publishTimeout().toMillis(), TimeUnit.MILLISECONDS);
            event.markPublished(OffsetDateTime.now(ZoneOffset.UTC));
            log.info("Alert event published eventId={} eventType={} vehicleId={}",
                    event.getEventId(), event.getEventType(), event.getVehicleId());
        } catch (JsonProcessingException exception) {
            event.markFailed(exception.getMessage());
            log.error("Alert serialization failed eventId={}", event.getEventId(), exception);
        } catch (Exception exception) {
            event.markFailed(exception.getMessage());
            log.warn("Alert publication failed eventId={} attempt={}",
                    event.getEventId(), event.getAttempts(), exception);
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
