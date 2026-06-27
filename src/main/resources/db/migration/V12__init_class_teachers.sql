-- =====================================================================
-- V12: Phan cong Giao vien <-> Khoa hoc (N:N)
-- 1 giao vien day nhieu khoa; 1 khoa co nhieu giao vien phu trach
-- Dung CLASS:READ (xem) / CLASS:WRITE (gan khi tao/sua khoa)
-- =====================================================================

CREATE TABLE class_teachers (
    class_id BIGINT NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    user_id  BIGINT NOT NULL REFERENCES users (id)   ON DELETE CASCADE,
    PRIMARY KEY (class_id, user_id)
);

CREATE INDEX idx_class_teachers_user  ON class_teachers (user_id);
CREATE INDEX idx_class_teachers_class ON class_teachers (class_id);
