-- =====================================================================
-- V11: Index ho tro tim kiem nguoi dung (full_name, phone, status)
-- =====================================================================

CREATE INDEX idx_users_full_name ON users (LOWER(full_name));
CREATE INDEX idx_users_phone     ON users (phone);
CREATE INDEX idx_users_status    ON users (status);
