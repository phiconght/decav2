-- -------------------------------------------------------------------
-- Lien ket De thi <-> Buoi hoc (bao cao cap 3 - theo buoi hoc)
-- Xem docs plan "Bao cao 3 cap do".
--
-- 1 buoi hoc : N de thi. Cot NULLABLE -> de thi cu khong co du lieu
-- buoi cho den khi duoc gan thu cong, khong backfill (khong suy luan
-- duoc buoi tu du lieu cu).
-- ON DELETE SET NULL: xoa buoi hoc KHONG duoc lam mat de thi.
--
-- GIOI HAN DA BIET: exams quan he N:N voi classes qua exam_classes,
-- nhung session_id la 1 FK duy nhat tren exams -> neu 1 de gan cho
-- nhieu lop, chi luu duoc session cua 1 lop. ExamService se canh bao
-- (khong chan) khi dieu nay xay ra.
-- -------------------------------------------------------------------

ALTER TABLE exams ADD COLUMN session_id BIGINT NULL REFERENCES class_sessions (id) ON DELETE SET NULL;

CREATE INDEX idx_exams_session ON exams (session_id);
