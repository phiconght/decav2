-- =====================================================================
-- V26: Bai lam cua hoc vien — luu JSON cau tra loi ngay tren exam_student
-- (1 cot dung chung cho ban nhap khi DANG_KIEM_TRA va ban nop khi DA_LAM)
-- Dinh dang: {"mc":{"<examExerciseId>":optionId},
--             "tf":{"<examExerciseId>":{"<tfItemId>":true|false}},
--             "essay":{"<examExerciseId>":"noi dung"}}
-- =====================================================================

ALTER TABLE exam_student ADD COLUMN answers TEXT;
