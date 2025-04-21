package com.inventory_kafka_streams.inventory_streams.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory_kafka_streams.inventory_streams.Dto.InventoryAggregate;
import com.inventory_kafka_streams.inventory_streams.model.InventoryEvent;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class SerdeConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public Serde<InventoryEvent> inventoryEventSerde(ObjectMapper objectMapper) {
        return new CustomJsonSerde<>(InventoryEvent.class, objectMapper);
    }

    @Bean
    public Serde<InventoryAggregate> inventoryAggregateSerde(ObjectMapper objectMapper) {
        return new CustomJsonSerde<>(InventoryAggregate.class, objectMapper);
    }

    public static class CustomJsonSerde<T> implements Serde<T> {
        private final JsonSerializer<T> serializer;
        private final JsonDeserializer<T> deserializer;

        public CustomJsonSerde(Class<T> targetType, ObjectMapper objectMapper) {
            this.serializer = new JsonSerializer<>(objectMapper);
            this.deserializer = new JsonDeserializer<>(targetType, objectMapper);
            this.deserializer.addTrustedPackages("com.inventory_kafka_streams.inventory_streams.*");
            this.deserializer.setUseTypeHeaders(false);
        }

        @Override
        public Serializer<T> serializer() {
            return serializer;
        }

        @Override
        public Deserializer<T> deserializer() {
            return deserializer;
        }
    }
}

