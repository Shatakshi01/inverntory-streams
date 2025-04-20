package com.inventory_kafka_streams.inventory_streams.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory_kafka_streams.inventory_streams.model.InventoryEvent;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryProducer {

    private static final Logger logger = LoggerFactory.getLogger(InventoryProducer.class);

    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;


    public InventoryProducer(KafkaTemplate<String, InventoryEvent> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void sendInventoryData() {
        logger.info("🔄 Reading inventory.json file to send events to {} topic...", "inventory");

        try (InputStream inputStream = new ClassPathResource("inventory.json").getInputStream()) {
            List<InventoryEvent> events = Arrays.asList(objectMapper.readValue(inputStream, InventoryEvent[].class));

            logger.info("📋 Loaded {} inventory events to send", events.size());

            for (InventoryEvent event : events) {
                // Use productId|warehouseId as the key for consistent routing
                String key = event.getProductId();

                try {
                    kafkaTemplate.send("inventory", key, event).get(); // Using get() to make it synchronous

                    logger.info("✅ Sent event → Key: {}, Product: {}, Warehouse: {}, Quantity: {}",
                        key, event.getProductId(), event.getWarehouseId(), event.getQuantity());

                    // Simulate delay between events
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("❌ Failed to send event: {}", event, e);
                }
            }

            logger.info("✅ Finished sending all inventory events");
        } catch (IOException e) {
            logger.error("❌ Failed to read inventory.json file", e);
        } catch (Exception e) {
            logger.error("❌ Unexpected error when sending inventory events", e);
        }
    }
}