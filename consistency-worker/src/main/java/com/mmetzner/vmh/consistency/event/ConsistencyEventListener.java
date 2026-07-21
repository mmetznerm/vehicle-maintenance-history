package com.mmetzner.vmh.consistency.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsistencyEventListener {

    private final ConsistencyEventProcessor processor;

    @KafkaListener(topics = "${app.kafka.input-topic}", groupId = "${app.kafka.consumer-group}")
    public void onEvent(String eventJson) {
        processor.process(eventJson);
    }
}
