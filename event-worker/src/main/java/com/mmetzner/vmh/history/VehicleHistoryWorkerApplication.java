package com.mmetzner.vmh.history;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class VehicleHistoryWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VehicleHistoryWorkerApplication.class, args);
    }
}
