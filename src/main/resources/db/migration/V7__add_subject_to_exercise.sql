-- =====================================================================
-- V7: Lien ket Bai Tap -> Mon Hoc
-- - Them cot subject_id FK vao exercises
-- - Xoa cot subject va grade_level (thong tin lay tu bang subjects)
-- =====================================================================

-- 1. Them cot subject_id (cho phep NULL tam thoi de backfill)
ALTER TABLE exercises ADD COLUMN subject_id BIGINT;

-- 2. Neu co du lieu cu: gan subject_id = NULL (cac ban ghi se bi xoa hoac xu ly tay)
--    Trong moi truong dev/test khong can backfill; prod can chay script rieng truoc V7.

-- 3. Them FK constraint
ALTER TABLE exercises
    ADD CONSTRAINT fk_exercises_subject
    FOREIGN KEY (subject_id) REFERENCES subjects (id);

-- 4. Dat NOT NULL sau khi backfill xong
ALTER TABLE exercises ALTER COLUMN subject_id SET NOT NULL;

-- 5. Index cho subject_id (ho tro JOIN va filter)
CREATE INDEX idx_exercises_subject_id ON exercises (subject_id);

-- 6. Xoa cot cu khong con dung
ALTER TABLE exercises DROP COLUMN subject;
ALTER TABLE exercises DROP COLUMN grade_level;
