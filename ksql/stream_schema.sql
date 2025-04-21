CREATE STREAM inventory_events_raw_ksql (
    productId VARCHAR KEY,
    productName VARCHAR,
    warehouseId VARCHAR,
    quantity INT
) WITH (
    KAFKA_TOPIC='inventory_ksql',
    VALUE_FORMAT='JSON',
    PARTITIONS=1,
    REPLICAS=1
);


CREATE STREAM inventory_per_warehouse_ksql
WITH (
    KAFKA_TOPIC='inventory-per-warehouse_ksql',
    VALUE_FORMAT='JSON'
) AS
SELECT
        productId + '|' + warehouseId AS KEY,
    productId,
    warehouseId,
    quantity AS totalQuantity
FROM inventory_events_raw_ksql
    PARTITION BY productId + '|' + warehouseId;


CREATE TABLE warehouse_stock_aggregate_ksql AS
SELECT
    warehouseId AS warehouse,
    SUM(quantity) AS total_stock
FROM inventory_events_raw_ksql
GROUP BY warehouseId;


CREATE TABLE low_stock_alerts_table_ksql
    WITH (
        KAFKA_TOPIC='low-stock-alerts_ksql',
        VALUE_FORMAT='JSON',
        KEY_FORMAT='JSON'
        ) AS
SELECT
        productId + '|' + warehouseId AS KEY,
    productId,
    warehouseId,
    SUM(quantity) AS totalQuantity
FROM inventory_events_raw_ksql
GROUP BY productId, warehouseId
HAVING SUM(quantity) < 30;


