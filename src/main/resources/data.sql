INSERT INTO inventory (sku,available_quantity,version) VALUES ('LAPTOP-001', 1, 0) ON DUPLICATE KEY UPDATE sku=sku;
