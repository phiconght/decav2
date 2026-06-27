-- =====================================================================
-- V18: Them do kho cho Bai tap (EASY | MEDIUM | HARD)
-- Cot NOT NULL DEFAULT 'MEDIUM' -> du lieu cu tu dien MEDIUM, an toan.
-- =====================================================================
ALTER TABLE exercises ADD COLUMN difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';
CREATE INDEX idx_exercises_difficulty ON exercises (difficulty);
