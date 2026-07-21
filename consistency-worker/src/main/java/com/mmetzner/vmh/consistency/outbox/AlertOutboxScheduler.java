package com.mmetzner.vmh.consistency.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class AlertOutboxScheduler {

    private final AlertOutboxRelay relay;

    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-interval:1s}")
    public void publishPendingEvents() {
        relay.publishPendingBatch();
    }
}
