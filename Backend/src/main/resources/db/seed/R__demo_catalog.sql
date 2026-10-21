-- Seed demo catalog — CHỈ nạp ở profile local (classpath:db/seed chỉ được thêm trong application-local.yml).
-- Đã chốt D-2-11 (O-2-01): không đặt seed trong migration versioned — tránh nhiễu integration test
-- (container PostgreSQL dùng chung mọi test class) và không chạy ở production.
-- Lưu ý: đổi schema bảng seed thì phải sửa file này cùng lúc (Flyway checksum).
-- CẤM reintroduce `spring.profiles.default: local` — deploy quên SPRING_PROFILES_ACTIVE sẽ nạp nhầm seed (liên hệ O-1-03).

INSERT INTO categories (name, description)
VALUES ('dien thoai', 'Dien thoai va phu kien'),
       ('laptop', 'Laptop, may tinh xach tay'),
       ('phu kien', 'Phu kien cong nghe')
ON CONFLICT (name) DO NOTHING;

INSERT INTO products (sku, name, description, price, status, category_id)
SELECT 'PHONE-001', 'Smartphone Nova X1', 'Man 6.5", 128GB', 4990000, 'ACTIVE', c.id
FROM categories c WHERE c.name = 'dien thoai'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO products (sku, name, description, price, status, category_id)
SELECT 'PHONE-002', 'Smartphone Nova Lite', 'Man 6.1", 64GB', 2990000, 'ACTIVE', c.id
FROM categories c WHERE c.name = 'dien thoai'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO products (sku, name, description, price, status, category_id)
SELECT 'LAPTOP-001', 'Laptop Aero 14', 'Core i5, 16GB, 512GB', 15990000, 'ACTIVE', c.id
FROM categories c WHERE c.name = 'laptop'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO products (sku, name, description, price, status, category_id)
SELECT 'LAPTOP-002', 'Laptop Aero 16 Pro', 'Core i7, 32GB, 1TB', 29990000, 'ACTIVE', c.id
FROM categories c WHERE c.name = 'laptop'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO products (sku, name, description, price, status, category_id)
SELECT 'ACC-001', 'Tai nghe SoundPods', 'Bluetooth 5.3', 1290000, 'INACTIVE', c.id
FROM categories c WHERE c.name = 'phu kien'
ON CONFLICT (sku) DO NOTHING;

INSERT INTO product_inventory (product_id, quantity)
SELECT p.id, 50 FROM products p
WHERE p.sku IN ('PHONE-001', 'PHONE-002', 'LAPTOP-001', 'LAPTOP-002')
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO product_inventory (product_id, quantity)
SELECT p.id, 0 FROM products p WHERE p.sku = 'ACC-001'
ON CONFLICT (product_id) DO NOTHING;
