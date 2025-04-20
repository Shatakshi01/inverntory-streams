package com.inventory_kafka_streams.inventory_streams;

import com.inventory_kafka_streams.inventory_streams.Dto.InventoryAggregate;
import com.inventory_kafka_streams.inventory_streams.model.InventoryEvent;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KGroupedStream;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InventoryProcessor {

    private static final Logger log = LoggerFactory.getLogger(InventoryProcessor.class);


    @Bean
    public KStream<String, InventoryEvent> process(
        StreamsBuilder builder,
        Serde<InventoryEvent> inventoryEventSerde,
        Serde<InventoryAggregate> inventoryAggregateSerde) {

        log.info("🔧 Setting up Kafka Streams Topology... Input: inventory, Output: inventory-per-warehouse , Alert: low-stock-alerts");

        KStream<String, InventoryEvent> stream = builder.stream(
            "inventory",
            Consumed.with(Serdes.String(), inventoryEventSerde)
        );

        stream.peek((key, value) ->
            log.info("📦 Received InventoryEvent: Key={}, ProductId={}, WarehouseId={}, Quantity={}",
                key, value.getProductId(), value.getWarehouseId(), value.getQuantity())
        );

        // Create a composite key for grouping by productId and warehouseId
        KGroupedStream<String, InventoryEvent> groupedStream = stream
            .groupBy((key, value) -> value.getProductId() + "|" + value.getWarehouseId(),
                Grouped.with(Serdes.String(), inventoryEventSerde));

        // Aggregate inventory per warehouse
        KTable<String, InventoryAggregate> stockPerWarehouse = groupedStream.aggregate(
            () -> InventoryAggregate.builder().totalQuantity(0).build(),
            (key, value, aggregate) -> {
                String[] keyParts = key.split("\\|");
                String productId = keyParts[0];
                String warehouseId = keyParts[1];

                int updatedStock = aggregate.getTotalQuantity() + value.getQuantity();

                log.info("📊 Aggregating for {}: {} => New stock: {}",
                    key, value.getQuantity(), updatedStock);

                return InventoryAggregate.builder()
                    .productId(productId)
                    .warehouseId(warehouseId)
                    .totalQuantity(updatedStock)
                    .build();
            },
            Materialized.with(Serdes.String(), inventoryAggregateSerde)
        );

        // Send aggregated stock to the output topic
        stockPerWarehouse
            .toStream()
            .peek((key, value) ->
                log.info("📤 Sending to {}: Key={}, ProductId={}, WarehouseId={}, TotalStock={}",
                   "inventory-per-warehouse", key, value.getProductId(), value.getWarehouseId(), value.getTotalQuantity())
            )
            .to("inventory-per-warehouse", Produced.with(Serdes.String(), inventoryAggregateSerde));

        // Generate low stock alerts
        stockPerWarehouse
            .toStream()
            .filter((key, value) -> value.getTotalQuantity() < 30)
            .peek((key, value) ->
                log.warn("🚨 Low stock alert for {} => Remaining stock: {}",
                    key, value.getTotalQuantity())
            )
            .to("low-stock-alerts", Produced.with(Serdes.String(), inventoryAggregateSerde));

        return stream;
    }
}