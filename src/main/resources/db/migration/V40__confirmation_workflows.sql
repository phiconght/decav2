-- =====================================================================
-- V40: 3 luong xac nhan (yeu cau nguoi dung 13/08/2026)
--   1) Diem danh (session_attendance)  -> GV/nhan vien/Admin xac nhan
--   2) Don xin nghi (leave_requests)   -> Phu huynh xac nhan TRUOC, sau do
--      GV/nhan vien moi duyet duoc (Admin duyet bat ky luc nao, bo qua)
--   3) Bai thi da nop (exam_student)   -> Nhan vien/GV xac nhan moi tinh
--      vao bao cao
-- =====================================================================

-- ---- 1) Diem danh ----
ALTER TABLE session_attendance
    ADD COLUMN confirmed_by BIGINT REFERENCES users (id),
    ADD COLUMN confirmed_at TIMESTAMPTZ;

-- Backfill: du lieu diem danh co TU TRUOC khi co tinh nang nay coi nhu da
-- xac nhan (confirmed_by NULL = he thong tu dong, khong phai 1 nguoi cu the)
-- — tranh bao cao lich su bien mat sau khi deploy (yeu cau nguoi dung 13/08/2026).
-- Chi ap dung cho luong xac nhan MOI tu day tro di.
UPDATE session_attendance SET confirmed_at = now() WHERE confirmed_at IS NULL;

CREATE INDEX idx_session_attendance_confirmed ON session_attendance (confirmed_at);

INSERT INTO permissions (code, resource, action, description) VALUES
    ('ATTENDANCE:CONFIRM', 'ATTENDANCE', 'CONFIRM', 'Xac nhan diem danh');

-- ADMIN + TEACHER + EMPLOYEE: xac nhan diem danh
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('ADMIN', 'TEACHER', 'EMPLOYEE') AND p.code = 'ATTENDANCE:CONFIRM';

-- ---- 2) Don xin nghi ----
ALTER TABLE leave_requests
    ADD COLUMN parent_confirmed_by BIGINT REFERENCES users (id),
    ADD COLUMN parent_confirmed_at TIMESTAMPTZ;

-- Backfill: don nghi da co tu truoc (ke ca dang PENDING) coi nhu da duoc PH
-- xac nhan, tranh chan duyet nhung don GV/nhan vien dang xu ly do (13/08/2026).
UPDATE leave_requests SET parent_confirmed_at = now() WHERE parent_confirmed_at IS NULL;

INSERT INTO permissions (code, resource, action, description) VALUES
    ('LEAVE:CONFIRM', 'LEAVE', 'CONFIRM', 'Phu huynh xac nhan don nghi cua con');

-- PARENT: xac nhan don nghi cua con
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'PARENT' AND p.code = 'LEAVE:CONFIRM';

-- EMPLOYEE truoc day chua co LEAVE:APPROVE — nay them de nhan vien cung
-- duyet duoc don nghi (yeu cau nguoi dung 13/08/2026).
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'EMPLOYEE' AND p.code = 'LEAVE:APPROVE'
  AND NOT EXISTS (
        SELECT 1 FROM role_permissions rp
        WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'EMPLOYEE' AND p.code = 'LEAVE:READ'
  AND NOT EXISTS (
        SELECT 1 FROM role_permissions rp
        WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- ---- 3) Bai thi da nop ----
ALTER TABLE exam_student
    ADD COLUMN confirmed_by BIGINT REFERENCES users (id),
    ADD COLUMN confirmed_at TIMESTAMPTZ;

-- Backfill: bai thi DA_LAM tu truoc khi co tinh nang nay coi nhu da xac
-- nhan — tranh bao cao diem/xep hang/pho diem bien mat sau khi deploy
-- (yeu cau nguoi dung 13/08/2026). Chi bai DA_LAM moi can (cac trang thai
-- khac khong bi loc theo confirmed_at trong bao cao).
UPDATE exam_student SET confirmed_at = now()
WHERE confirmed_at IS NULL AND status = 'DA_LAM';

CREATE INDEX idx_exam_student_confirmed ON exam_student (confirmed_at);

INSERT INTO permissions (code, resource, action, description) VALUES
    ('EXAM:CONFIRM', 'EXAM', 'CONFIRM', 'Xac nhan bai thi da nop de tinh vao bao cao');

-- ADMIN + TEACHER + EMPLOYEE: xac nhan bai thi
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('ADMIN', 'TEACHER', 'EMPLOYEE') AND p.code = 'EXAM:CONFIRM';
