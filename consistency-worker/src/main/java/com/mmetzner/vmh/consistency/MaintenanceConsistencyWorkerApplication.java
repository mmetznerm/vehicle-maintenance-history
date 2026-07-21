package com.mmetzner.vmh.consistency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableKafka
@EnableScheduling
@SpringBootApplication
public class MaintenanceConsistencyWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaintenanceConsistencyWorkerApplication.class, args);
    }
}
