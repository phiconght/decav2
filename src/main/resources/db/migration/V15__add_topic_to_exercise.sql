-- =====================================================================
-- V15: Lien ket Bai Tap -> Chuyen de (topic_id, cho phep NULL)
-- Bai tap cu khong co chuyen de van giu NULL; bai moi se chon tu giao dien.
-- =====================================================================

ALTER TABLE exercises ADD COLUMN topic_id BIGINT;

ALTER TABLE exercises
    ADD CONSTRAINT fk_exercises_topic
    FOREIGN KEY (topic_id) REFERENCES topics (id);

CREATE INDEX idx_exercises_topic ON exercises (topic_id);
