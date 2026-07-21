package com.mmetzner.vmh.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.mmetzner.vmh.shared.event.OutboxRelayProperties;

@EnableKafka
@EnableScheduling
@Configuration
@EnableConfigurationProperties(OutboxRelayProperties.class)
public class KafkaConfig {

    @Bean
    NewTopic vehicleMaintenanceEventsTopic(
            @Value("${app.kafka.topic.name:vehicle-maintenance-events.v1}") String name,
            @Value("${app.kafka.topic.partitions:3}") int partitions,
            @Value("${app.kafka.topic.replication-factor:1}") short replicationFactor
    ) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    NewTopic maintenanceInconsistencyAlertsTopic(
            @Value("${app.kafka.alert-topic.name:maintenance-inconsistency-alerts.v1}") String name,
            @Value("${app.kafka.alert-topic.partitions:3}") int partitions,
            @Value("${app.kafka.alert-topic.replication-factor:1}") short replicationFactor
    ) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}
