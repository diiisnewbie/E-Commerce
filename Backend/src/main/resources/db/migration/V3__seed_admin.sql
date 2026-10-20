-- Phase 1: seed tài khoản ADMIN đầu tiên (D-1-08).
-- Hash đọc từ Flyway placeholder admin_password_hash (spring.flyway.placeholders.*) — không hardcode secret trong SQL.
-- ON CONFLICT: chỉ có tác dụng lần migrate đầu trên DB trống; checksum của Flyway giữ migration này chạy đúng 1 lần.
INSERT INTO users (email, password_hash, full_name, role)
VALUES ('admin@ecommerce.dev', '${admin_password_hash}', 'System Admin', 'ADMIN')
ON CONFLICT (email) DO NOTHING;
