-- =====================================================================
-- V16: Lien ket De thi -> Chuyen de (topic_id, cho phep NULL)
-- 1 de gan toi da 1 chuyen de; co the khong gan.
-- =====================================================================

ALTER TABLE exams ADD COLUMN topic_id BIGINT;

ALTER TABLE exams
    ADD CONSTRAINT fk_exams_topic
    FOREIGN KEY (topic_id) REFERENCES topics (id);

CREATE INDEX idx_exams_topic ON exams (topic_id);
