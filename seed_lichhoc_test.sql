-- =====================================================================
-- SEED DỮ LIỆU TEST cho module LỊCH HỌC (branches/rooms/holidays/
--   student_parents/class_schedules/class_sessions/session_attendance/
--   leave_requests/notifications)
--
-- YÊU CẦU: chạy SAU KHI restart BE (để Flyway tạo bảng V20–V25).
-- Re-runnable: tự xóa phần created_by='seed-lichhoc' rồi nạp lại.
-- Tái dùng dữ liệu thật: users (PARENT/STUDENT/TEACHER), classes ACTIVE, class_students.
--
-- Chạy:
--   $env:PGPASSWORD="center"
--   & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -h localhost -U center -d center -v ON_ERROR_STOP=1 -f BE\seed_lichhoc_test.sql
-- =====================================================================
\encoding UTF8
BEGIN;

-- ---------- DỌN LẦN CHẠY TRƯỚC (đúng thứ tự FK) ----------
DELETE FROM session_attendance     WHERE created_by = 'seed-lichhoc';
DELETE FROM leave_requests          WHERE created_by = 'seed-lichhoc';
DELETE FROM notifications           WHERE created_by = 'seed-lichhoc';
DELETE FROM class_sessions          WHERE created_by = 'seed-lichhoc';
DELETE FROM class_schedules         WHERE created_by = 'seed-lichhoc';
DELETE FROM student_parents         WHERE created_by = 'seed-lichhoc';
DELETE FROM holidays                WHERE created_by = 'seed-lichhoc';
DELETE FROM rooms                   WHERE created_by = 'seed-lichhoc';
DELETE FROM branches                WHERE created_by = 'seed-lichhoc';
-- (class_teachers là bảng nối thuần, không có created_by → không tự xóa; chèn ON CONFLICT DO NOTHING.)

-- ---------- 1) CƠ SỞ ----------
INSERT INTO branches (code, name, address, active, created_at, created_by) VALUES
  ('CS1', 'Cơ sở Quận 1',  '12 Nguyễn Huệ, Quận 1',     TRUE, now(), 'seed-lichhoc'),
  ('CS2', 'Cơ sở Thủ Đức', '45 Võ Văn Ngân, Thủ Đức',   TRUE, now(), 'seed-lichhoc');

-- ---------- 2) PHÒNG HỌC (6 phòng, 2 cơ sở) ----------
INSERT INTO rooms (code, name, branch_id, capacity, note, active, created_at, created_by)
SELECT v.code, v.name, b.id, v.cap, NULL, TRUE, now(), 'seed-lichhoc'
FROM (VALUES
    ('P101', 'Phòng 101', 'CS1', 30),
    ('P102', 'Phòng 102', 'CS1', 25),
    ('P201', 'Phòng 201', 'CS1', 40),
    ('TD-A', 'Phòng A',   'CS2', 30),
    ('TD-B', 'Phòng B',   'CS2', 20),
    ('TD-C', 'Phòng C',   'CS2', 35)
) AS v(code, name, bcode, cap)
JOIN branches b ON b.code = v.bcode AND b.created_by = 'seed-lichhoc';

-- ---------- 3) NGÀY NGHỈ ----------
INSERT INTO holidays (holiday_date, name, branch_id, created_at, created_by) VALUES
  (DATE '2026-09-02', 'Quốc khánh 02/09', NULL, now(), 'seed-lichhoc'),
  (CURRENT_DATE + 3,  'Nghỉ lễ thử nghiệm', NULL, now(), 'seed-lichhoc');

-- ---------- 4) LIÊN KẾT HỌC VIÊN ↔ PHỤ HUYNH ----------
-- Ghép phụ huynh rn với học viên rn (1:1) cho 20 PH; 5 PH đầu thêm 1 con nữa (PH nhiều con).
WITH parents AS (
    SELECT u.id, row_number() OVER (ORDER BY u.id) AS rn
    FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
    WHERE r.name = 'PARENT'
),
students AS (
    SELECT u.id, row_number() OVER (ORDER BY u.id) AS rn
    FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
    WHERE r.name = 'STUDENT'
)
INSERT INTO student_parents (student_id, parent_id, relationship, created_at, created_by)
SELECT s.id, p.id, (ARRAY['FATHER','MOTHER','GUARDIAN'])[1 + (p.rn % 3)], now(), 'seed-lichhoc'
FROM parents p JOIN students s ON s.rn = p.rn
WHERE p.rn <= 20
ON CONFLICT (student_id, parent_id) DO NOTHING;

WITH parents AS (
    SELECT u.id, row_number() OVER (ORDER BY u.id) AS rn
    FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
    WHERE r.name = 'PARENT'
),
students AS (
    SELECT u.id, row_number() OVER (ORDER BY u.id) AS rn
    FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
    WHERE r.name = 'STUDENT'
)
INSERT INTO student_parents (student_id, parent_id, relationship, created_at, created_by)
SELECT s.id, p.id, 'MOTHER', now(), 'seed-lichhoc'
FROM parents p JOIN students s ON s.rn = p.rn + 50
WHERE p.rn <= 5
ON CONFLICT (student_id, parent_id) DO NOTHING;

-- ---------- 5) GÁN GIÁO VIÊN CHO LỚP (mỗi lớp ACTIVE 1 GV, round-robin) ----------
WITH ac AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn FROM classes WHERE status = 'ACTIVE'
),
teachers AS (
    SELECT u.id, row_number() OVER (ORDER BY u.id) AS rn
    FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
    WHERE r.name = 'TEACHER'
),
tcount AS (SELECT count(*) AS c FROM teachers)
INSERT INTO class_teachers (class_id, user_id)
SELECT ac.id, t.id
FROM ac
CROSS JOIN tcount
JOIN teachers t ON t.rn = (ac.rn % tcount.c) + 1
ON CONFLICT (class_id, user_id) DO NOTHING;

-- ---------- 6) QUY TẮC LỊCH (WEEKLY) cho 10 lớp ACTIVE đầu tiên ----------
WITH ac AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn
    FROM classes WHERE status = 'ACTIVE' ORDER BY id LIMIT 10
),
rc AS (SELECT count(*) AS c FROM rooms WHERE created_by = 'seed-lichhoc'),
rms AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn FROM rooms WHERE created_by = 'seed-lichhoc'
)
INSERT INTO class_schedules
    (class_id, recurrence_type, day_of_week, start_date, end_date,
     start_time, duration_minutes, room_id, teacher_id, active, created_at, created_by)
SELECT
    ac.id,
    'WEEKLY',
    (ac.rn % 6) + 1,                                   -- T2..T7 (ISO 1..6)
    CURRENT_DATE - 7,
    CURRENT_DATE + 60,
    (TIME '07:30' + ((ac.rn % 5) * INTERVAL '90 minutes')),
    90,
    (SELECT id FROM rms CROSS JOIN rc WHERE rms.rn = (ac.rn % rc.c) + 1),
    (SELECT ct.user_id FROM class_teachers ct WHERE ct.class_id = ac.id LIMIT 1),
    TRUE, now(), 'seed-lichhoc'
FROM ac;

-- ---------- 7) SINH BUỔI HỌC từ quy tắc (từ start_date đến hôm nay+14, đúng thứ, bỏ ngày nghỉ) ----------
INSERT INTO class_sessions
    (class_id, schedule_id, session_date, start_time, duration_minutes,
     room_id, teacher_id, status, is_manual, created_at, created_by)
SELECT
    s.class_id, s.id, d::date, s.start_time, s.duration_minutes,
    s.room_id, s.teacher_id,
    CASE WHEN d::date < CURRENT_DATE THEN 'DONE' ELSE 'PLANNED' END,
    FALSE, now(), 'seed-lichhoc'
FROM class_schedules s
CROSS JOIN LATERAL generate_series(s.start_date, LEAST(s.end_date, CURRENT_DATE + 14), INTERVAL '1 day') AS d
WHERE s.created_by = 'seed-lichhoc'
  AND EXTRACT(ISODOW FROM d) = s.day_of_week
  AND d::date NOT IN (SELECT holiday_date FROM holidays)
ON CONFLICT (class_id, session_date, start_time) DO NOTHING;

-- ---------- 8) ĐIỂM DANH cho buổi đã/đang diễn ra (từng học viên trong lớp) ----------
INSERT INTO session_attendance (session_id, user_id, status, check_in_at, created_at, created_by)
SELECT
    cs.id, cstu.user_id,
    CASE (cstu.user_id + cs.id) % 5
        WHEN 0 THEN 'VANG'
        WHEN 1 THEN 'TRE'
        ELSE 'CO_MAT'
    END,
    CASE WHEN (cstu.user_id + cs.id) % 5 = 0 THEN NULL
         ELSE ((cs.session_date + cs.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh') END,
    now(), 'seed-lichhoc'
FROM class_sessions cs
JOIN class_students cstu ON cstu.class_id = cs.class_id
WHERE cs.created_by = 'seed-lichhoc'
  AND cs.session_date <= CURRENT_DATE
ON CONFLICT (session_id, user_id) DO NOTHING;

-- ---------- 9) ĐƠN NGHỈ PHÉP mẫu (PENDING để test nút Duyệt/Từ chối) ----------
-- 9a) Theo BUỔI (buổi tương lai gần nhất)
INSERT INTO leave_requests (student_id, requested_by, scope, session_id, reason, status, created_at, created_by)
SELECT cstu.user_id, cstu.user_id, 'SESSION', cs.id, 'Bận việc gia đình', 'PENDING', now(), 'seed-lichhoc'
FROM class_sessions cs
JOIN class_students cstu ON cstu.class_id = cs.class_id
WHERE cs.created_by = 'seed-lichhoc' AND cs.session_date > CURRENT_DATE
ORDER BY cs.session_date, cs.id
LIMIT 1;

-- 9b) Theo KHOẢNG NGÀY (1 lớp)
INSERT INTO leave_requests (student_id, requested_by, scope, class_id, date_from, date_to, reason, status, created_at, created_by)
SELECT cstu.user_id, cstu.user_id, 'RANGE', cs.class_id, CURRENT_DATE + 1, CURRENT_DATE + 5, 'Về quê có việc', 'PENDING', now(), 'seed-lichhoc'
FROM class_sessions cs
JOIN class_students cstu ON cstu.class_id = cs.class_id
WHERE cs.created_by = 'seed-lichhoc'
ORDER BY cs.class_id DESC, cstu.user_id
LIMIT 1;

-- ---------- 10) THÔNG BÁO mẫu cho 1 phụ huynh (test GET /notifications/me) ----------
INSERT INTO notifications (recipient_user_id, channel, type, title, body, payload, status, dedupe_key, retry_count, created_at, created_by)
SELECT p.id, 'PUSH', t.type, t.title, t.body, NULL, t.status, 'seed-lichhoc:' || t.type || ':' || p.id, 0, now(), 'seed-lichhoc'
FROM (
    SELECT u.id FROM users u JOIN user_roles ur ON ur.user_id = u.id JOIN roles r ON r.id = ur.role_id
    WHERE r.name = 'PARENT' ORDER BY u.id LIMIT 1
) p
CROSS JOIN (VALUES
    ('SESSION_REMINDER', 'Nhac lich hoc', 'Con ban co buoi hoc luc 18:00 hom nay.', 'SENT'),
    ('MISSING_CHECKIN',  'Thieu check-in', 'Con ban chua check-in buoi hoc hom nay.', 'PENDING'),
    ('LEAVE_RESULT',     'Don nghi duoc duyet', 'Don xin nghi cua con ban da duoc duyet.', 'SENT')
) AS t(type, title, body, status)
ON CONFLICT (dedupe_key) DO NOTHING;

COMMIT;

-- ---------- KIỂM TRA (bỏ comment để xem số lượng đã nạp) ----------
-- SELECT 'branches', count(*) FROM branches WHERE created_by='seed-lichhoc'
-- UNION ALL SELECT 'rooms', count(*) FROM rooms WHERE created_by='seed-lichhoc'
-- UNION ALL SELECT 'holidays', count(*) FROM holidays WHERE created_by='seed-lichhoc'
-- UNION ALL SELECT 'student_parents', count(*) FROM student_parents WHERE created_by='seed-lichhoc'
-- UNION ALL SELECT 'class_schedules', count(*) FROM class_schedules WHERE created_by='seed-lichhoc'
-- UNION ALL SELECT 'class_sessions', count(*) FROM class_sessions WHERE created_by='seed-lichhoc'
-- UNION ALL SELECT 'session_attendance', count(*) FROM session_attendance WHERE created_by='seed-lichhoc'
-- UNION ALL SELECT 'leave_requests', count(*) FROM leave_requests WHERE created_by='seed-lichhoc'
-- UNION ALL SELECT 'notifications', count(*) FROM notifications WHERE created_by='seed-lichhoc';
