package com.mmetzner.vmh.consistency.config;

import com.mmetzner.vmh.consistency.outbox.AlertOutboxProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(AlertOutboxProperties.class)
public class KafkaTopicConfig {

    @Bean
    NewTopic maintenanceInconsistencyAlertTopic(
            @Value("${app.kafka.output-topic}") String name,
            @Value("${app.kafka.output-partitions:3}") int partitions
    ) {
        return TopicBuilder.name(name).partitions(partitions).replicas(1).build();
    }
}
