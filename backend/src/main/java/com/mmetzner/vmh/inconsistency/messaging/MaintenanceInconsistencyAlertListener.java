package com.mmetzner.vmh.inconsistency.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MaintenanceInconsistencyAlertListener {

    private final MaintenanceInconsistencyAlertProcessor processor;

    @KafkaListener(
            topics = "${app.kafka.alert-topic.name}",
            groupId = "${app.kafka.alert-topic.consumer-group}"
    )
    public void onAlert(String eventJson) {
        processor.process(eventJson);
    }
}
