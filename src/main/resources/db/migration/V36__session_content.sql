-- -------------------------------------------------------------------
-- Noi dung buoi hoc: chuyen de + ten buoi
-- Xem docs/SPEC_KhoaHoc_NoiDung_Mobile.md §3.1
--
-- Muc dich: cho phep gom buoi hoc theo CHUYEN DE (topic) tren man
-- chi tiet khoa hoc o Mobile, thay vi 2 bang roi rac nhu truoc.
--
-- Ca 2 cot deu NULLABLE -> du lieu buoi hoc cu van hop le, khong can
-- backfill, khong vo `ddl-auto: validate`.
-- ON DELETE SET NULL: xoa chuyen de KHONG duoc lam mat buoi hoc,
-- buoi chi roi ve nhom "Chua phan chuyen de".
-- -------------------------------------------------------------------

ALTER TABLE class_sessions
    ADD COLUMN topic_id BIGINT NULL REFERENCES topics (id) ON DELETE SET NULL;

ALTER TABLE class_sessions
    ADD COLUMN title VARCHAR(255) NULL;

CREATE INDEX idx_class_sessions_topic ON class_sessions (topic_id);
