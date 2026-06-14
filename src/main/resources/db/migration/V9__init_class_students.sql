-- =====================================================================
-- V9: Ghi danh Lop <-> Hoc sinh (N:N)
-- 1 hoc sinh hoc nhieu lop; 1 lop nhieu hoc sinh
-- Khong them permission moi: dung CLASS:READ (xem) / CLASS:WRITE (add/xoa)
-- =====================================================================

CREATE TABLE class_students (
    class_id BIGINT NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    user_id  BIGINT NOT NULL REFERENCES users (id)   ON DELETE CASCADE,
    PRIMARY KEY (class_id, user_id)
);

CREATE INDEX idx_class_students_user  ON class_students (user_id);
CREATE INDEX idx_class_students_class ON class_students (class_id);
