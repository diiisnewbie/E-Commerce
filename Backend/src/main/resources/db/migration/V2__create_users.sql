-- Phase 1: bảng users (phase-1-auth.md mục 3.1).
-- CHECK email = lower(email): lớp phòng thủ thứ hai, service đã chuẩn hóa toLowerCase(Locale.ROOT) trước khi lưu.
-- Không tạo index cho cột role (chỉ 2 giá trị — over-engineering; uk_users_email đủ cho truy vấn chính).
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_email_lowercase CHECK (email = lower(email))
);
