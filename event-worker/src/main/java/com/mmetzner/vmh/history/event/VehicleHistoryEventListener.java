package com.mmetzner.vmh.history.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleHistoryEventListener {

    private final VehicleHistoryEventProcessor processor;

    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "${app.kafka.consumer-group}"
    )
    public void onEvent(String eventJson) {
        processor.process(eventJson);
    }
}
