INSERT INTO customers (id, name) VALUES (1, 'Alice');
INSERT INTO customers (id, name) VALUES (2, 'Bob');
INSERT INTO customers (id, name) VALUES (3, 'Carol');

INSERT INTO purchase_orders (id, customer_id, status, total, created_at, version) VALUES (1001, 1, 'NEW', 120.50, '2026-01-01T10:00:00Z', 0);
INSERT INTO purchase_orders (id, customer_id, status, total, created_at, version) VALUES (1002, 2, 'PAID', 210.00, '2026-01-02T11:00:00Z', 0);
INSERT INTO purchase_orders (id, customer_id, status, total, created_at, version) VALUES (1003, 1, 'SHIPPED', 99.99, '2026-01-03T12:00:00Z', 0);
INSERT INTO purchase_orders (id, customer_id, status, total, created_at, version) VALUES (1004, 3, 'PAID', 450.00, '2026-01-04T13:00:00Z', 0);
INSERT INTO purchase_orders (id, customer_id, status, total, created_at, version) VALUES (1005, 2, 'NEW', 80.00, '2026-01-05T14:00:00Z', 0);
INSERT INTO purchase_orders (id, customer_id, status, total, created_at, version) VALUES (1006, 3, 'CANCELLED', 50.00, '2026-01-06T15:00:00Z', 0);
INSERT INTO purchase_orders (id, customer_id, status, total, created_at, version) VALUES (1007, 1, 'PAID', 300.00, '2026-01-07T16:00:00Z', 0);
INSERT INTO purchase_orders (id, customer_id, status, total, created_at, version) VALUES (1008, 2, 'SHIPPED', 150.00, '2026-01-08T17:00:00Z', 0);
