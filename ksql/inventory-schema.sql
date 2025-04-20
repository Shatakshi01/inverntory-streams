CREATE STREAM inventory_events_raw (
  productId VARCHAR,
  warehouseId VARCHAR,
  quantity INT,
  timestamp VARCHAR
) WITH (
  KAFKA_TOPIC='inventory',
  VALUE_FORMAT='JSON'
);

-- Aggregate quantity per warehouse
CREATE TABLE inventory_per_warehouse AS
SELECT warehouseId, SUM(quantity) AS total_quantity
FROM inventory_events_raw
GROUP BY warehouseId;

-- Alert for low stock
CREATE STREAM low_stock_alerts AS
SELECT productId, warehouseId, quantity
FROM inventory_events_raw
WHERE quantity < 10;