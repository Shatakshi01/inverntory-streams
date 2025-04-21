package com.inventory_kafka_streams.inventory_streams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class InventoryStreamsApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryStreamsApplication.class, args);
	}

}
