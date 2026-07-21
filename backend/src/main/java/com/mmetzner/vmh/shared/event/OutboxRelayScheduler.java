package com.mmetzner.vmh.shared.event;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private final OutboxRelay relay;

    @Scheduled(fixedDelayString = "${app.kafka.outbox.poll-interval:1s}")
    public void publishPendingEvents() {
        relay.publishPendingBatch();
    }
}
