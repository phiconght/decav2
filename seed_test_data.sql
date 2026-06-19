-- =====================================================================
-- DỮ LIỆU TEST cho DEV (PostgreSQL) — module: user, bài tập, lớp học, đề thi
-- Re-runnable: mọi bản ghi đánh dấu created_by='seed-test', script tự xóa & tạo lại.
-- KHÔNG đụng tới admin, role, permission, subjects (do Flyway seed).
-- Mật khẩu mọi user test = Admin@123 (tái dùng bcrypt hash của admin).
-- Chạy: psql -h localhost -U center -d center -f seed_test_data.sql
-- =====================================================================
\encoding UTF8
BEGIN;

\set pwd '''$2a$10$2SYKYkFDomaRDaR8b6QCGORR88hYA9IYGi4UY6C4EniNVfpCfSK4S'''

-- ---------- 0. Dọn dữ liệu test cũ (đúng thứ tự FK) ----------
DELETE FROM exams      WHERE created_by = 'seed-test';   -- cascade exam_exercises, exam_tf_item_scores, exam_classes, exam_students
DELETE FROM classes    WHERE created_by = 'seed-test';   -- cascade class_students
DELETE FROM exercises  WHERE created_by = 'seed-test';   -- cascade choice_options, true_false_items
DELETE FROM users      WHERE created_by = 'seed-test';   -- cascade user_roles

-- =====================================================================
-- 1. MODULE USER — tài khoản + vai trò
-- =====================================================================
INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by) VALUES
  ('gv_toan',  'gvtoan@trungtam.vn',  '0901000001', :pwd, 'Nguyễn Văn Toán',   'ACTIVE',   now(), 'seed-test'),
  ('gv_anh',   'gvanh@trungtam.vn',   '0901000002', :pwd, 'Trần Thị Lan Anh',  'ACTIVE',   now(), 'seed-test'),
  ('gv_ly',    'gvly@trungtam.vn',    '0901000003', :pwd, 'Lê Văn Lý',         'ACTIVE',   now(), 'seed-test'),
  ('nv_vp',    'nvvp@trungtam.vn',    '0901000004', :pwd, 'Phạm Thị Hoa',      'ACTIVE',   now(), 'seed-test'),
  ('trogiang', 'tg@trungtam.vn',      '0901000005', :pwd, 'Đỗ Văn Trợ',        'ACTIVE',   now(), 'seed-test'),
  ('hs01',     'hs01@trungtam.vn',    '0902000001', :pwd, 'Vũ Minh Khôi',      'ACTIVE',   now(), 'seed-test'),
  ('hs02',     'hs02@trungtam.vn',    '0902000002', :pwd, 'Hoàng Thị Mai',     'ACTIVE',   now(), 'seed-test'),
  ('hs03',     'hs03@trungtam.vn',    '0902000003', :pwd, 'Đặng Quốc Bảo',     'ACTIVE',   now(), 'seed-test'),
  ('hs04',     'hs04@trungtam.vn',    '0902000004', :pwd, 'Bùi Thị Ngọc',      'ACTIVE',   now(), 'seed-test'),
  ('hs05',     'hs05@trungtam.vn',    '0902000005', :pwd, 'Ngô Văn Hùng',      'ACTIVE',   now(), 'seed-test'),
  ('hs06',     'hs06@trungtam.vn',    '0902000006', :pwd, 'Dương Thị Thu',     'ACTIVE',   now(), 'seed-test'),
  ('hs07',     'hs07@trungtam.vn',    '0902000007', :pwd, 'Lý Hoàng Nam',      'ACTIVE',   now(), 'seed-test'),
  ('hs08',     'hs08@trungtam.vn',    '0902000008', :pwd, 'Phan Thị Linh',     'DISABLED', now(), 'seed-test'),
  ('ph01',     'ph01@trungtam.vn',    '0903000001', :pwd, 'Vũ Văn Phụ',        'ACTIVE',   now(), 'seed-test'),
  ('ph02',     NULL,                  '0903000002', :pwd, 'Hoàng Văn Huynh',   'LOCKED',   now(), 'seed-test');

-- Gán vai trò (username -> role name)
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM (VALUES
  ('gv_toan','TEACHER'), ('gv_anh','TEACHER'), ('gv_ly','TEACHER'),
  ('nv_vp','EMPLOYEE'), ('trogiang','ASSISTANT'),
  ('hs01','STUDENT'),('hs02','STUDENT'),('hs03','STUDENT'),('hs04','STUDENT'),
  ('hs05','STUDENT'),('hs06','STUDENT'),('hs07','STUDENT'),('hs08','STUDENT'),
  ('ph01','PARENT'),('ph02','PARENT')
) AS m(uname, rolename)
JOIN users u ON u.username = m.uname
JOIN roles r ON r.name = m.rolename;

-- =====================================================================
-- 2. MODULE BÀI TẬP (exercise) — 3 loại: MULTIPLE_CHOICE / ESSAY / TRUE_FALSE
--    subject_id: Toán K11=6, Toán K10=5, Tiếng Anh K10=33, Vật lý K11=13
-- =====================================================================
INSERT INTO exercises (code, title, subject_id, type, status, question_text, essay_answer, created_at, created_by) VALUES
  ('BTO11-00001N', 'Đạo hàm hàm số mũ',       6,  'MULTIPLE_CHOICE', 'ACTIVE',   'Đạo hàm của hàm số y = e^x là?',                            NULL, now(), 'seed-test'),
  ('BTO11-00002N', 'Giới hạn dãy số',          6,  'MULTIPLE_CHOICE', 'ACTIVE',   'Giới hạn của dãy số u(n) = 1/n khi n → ∞ bằng?',          NULL, now(), 'seed-test'),
  ('BTO11-00003L', 'Chứng minh bất đẳng thức', 6,  'ESSAY',           'ACTIVE',   'Chứng minh rằng với mọi a, b > 0: a/b + b/a ≥ 2.',         'Áp dụng BĐT Cauchy: a/b + b/a ≥ 2·√(a/b · b/a) = 2. Dấu "=" khi a = b.', now(), 'seed-test'),
  ('BTO11-00004D', 'Mệnh đề logic đúng/sai',   6,  'TRUE_FALSE',      'ACTIVE',   'Xét tính đúng/sai của các mệnh đề về số thực sau:',        NULL, now(), 'seed-test'),
  ('BTO10-00001N', 'Hàm số bậc hai',           5,  'MULTIPLE_CHOICE', 'ACTIVE',   'Parabol y = x² - 4x + 3 có tọa độ đỉnh là?',              NULL, now(), 'seed-test'),
  ('BAN10-00001N', 'Choose the correct tense', 33, 'MULTIPLE_CHOICE', 'ACTIVE',   'She ____ to school every day.',                            NULL, now(), 'seed-test'),
  ('BAN10-00002L', 'Write about your hobby',   33, 'ESSAY',           'INACTIVE', 'Write a short paragraph (about 80 words) about your hobby.','A sample answer describing a hobby such as reading or football.', now(), 'seed-test'),
  ('BLY11-00001N', 'Định luật Ôm',             13, 'MULTIPLE_CHOICE', 'ACTIVE',   'Công thức của định luật Ôm cho đoạn mạch là?',            NULL, now(), 'seed-test'),
  ('BLY11-00002D', 'Nhận định về điện trường', 13, 'TRUE_FALSE',      'ACTIVE',   'Xét tính đúng/sai của các nhận định về điện trường:',     NULL, now(), 'seed-test');

-- 2a. Đáp án trắc nghiệm (mỗi bài đúng 1 is_correct=true)
INSERT INTO choice_options (exercise_id, sort_order, text, is_correct)
SELECT e.id, v.so, v.txt, v.ok
FROM (VALUES
  ('BTO11-00001N', 0, 'y'' = e^x',           TRUE),
  ('BTO11-00001N', 1, 'y'' = x·e^(x-1)',     FALSE),
  ('BTO11-00001N', 2, 'y'' = e^(x-1)',       FALSE),
  ('BTO11-00001N', 3, 'y'' = ln(x)',         FALSE),
  ('BTO11-00002N', 0, '0',                   TRUE),
  ('BTO11-00002N', 1, '1',                   FALSE),
  ('BTO11-00002N', 2, '∞',                   FALSE),
  ('BTO11-00002N', 3, 'Không tồn tại',       FALSE),
  ('BTO10-00001N', 0, '(2; -1)',             TRUE),
  ('BTO10-00001N', 1, '(-2; -1)',            FALSE),
  ('BTO10-00001N', 2, '(2; 1)',              FALSE),
  ('BTO10-00001N', 3, '(4; 3)',              FALSE),
  ('BAN10-00001N', 0, 'goes',                TRUE),
  ('BAN10-00001N', 1, 'go',                  FALSE),
  ('BAN10-00001N', 2, 'going',               FALSE),
  ('BAN10-00001N', 3, 'gone',                FALSE),
  ('BLY11-00001N', 0, 'I = U / R',           TRUE),
  ('BLY11-00001N', 1, 'I = U · R',           FALSE),
  ('BLY11-00001N', 2, 'I = R / U',           FALSE),
  ('BLY11-00001N', 3, 'U = I / R',           FALSE)
) AS v(code, so, txt, ok)
JOIN exercises e ON e.code = v.code;

-- 2b. Các ý đúng/sai
INSERT INTO true_false_items (exercise_id, sort_order, text, answer)
SELECT e.id, v.so, v.txt, v.ans
FROM (VALUES
  ('BTO11-00004D', 0, 'Số 0 là số thực.',                       TRUE),
  ('BTO11-00004D', 1, 'Mọi số tự nhiên đều là số nguyên.',      TRUE),
  ('BTO11-00004D', 2, 'Căn bậc hai của -1 là số thực.',         FALSE),
  ('BTO11-00004D', 3, 'Số π là số hữu tỉ.',                     FALSE),
  ('BLY11-00002D', 0, 'Điện trường là trường vectơ.',           TRUE),
  ('BLY11-00002D', 1, 'Đường sức điện trường là đường khép kín.',FALSE),
  ('BLY11-00002D', 2, 'Điện tích cùng dấu thì đẩy nhau.',       TRUE)
) AS v(code, so, txt, ans)
JOIN exercises e ON e.code = v.code;

-- =====================================================================
-- 3. MODULE LỚP HỌC (class) + ghi danh học sinh (class_students)
-- =====================================================================
INSERT INTO classes (code, name, subject_id, start_date, end_date, status, created_at, created_by) VALUES
  ('CTO11-00001L', 'Toán 11A1',   6,  DATE '2026-01-15', DATE '2026-05-30', 'ACTIVE',   now(), 'seed-test'),
  ('CTO10-00001L', 'Toán 10A2',   5,  DATE '2026-02-01', DATE '2026-06-15', 'ACTIVE',   now(), 'seed-test'),
  ('CAN10-00001L', 'Anh văn 10B1',33, DATE '2026-01-20', DATE '2026-05-20', 'ACTIVE',   now(), 'seed-test'),
  ('CLY11-00001L', 'Lý 11A1',     13, DATE '2025-09-05', DATE '2026-01-10', 'INACTIVE', now(), 'seed-test');

-- Ghi danh: học sinh vào lớp
INSERT INTO class_students (class_id, user_id)
SELECT c.id, u.id
FROM (VALUES
  ('CTO11-00001L','hs01'),('CTO11-00001L','hs02'),('CTO11-00001L','hs03'),('CTO11-00001L','hs04'),('CTO11-00001L','hs05'),
  ('CTO10-00001L','hs06'),('CTO10-00001L','hs07'),
  ('CAN10-00001L','hs01'),('CAN10-00001L','hs02'),('CAN10-00001L','hs03'),('CAN10-00001L','hs06'),
  ('CLY11-00001L','hs04'),('CLY11-00001L','hs05')
) AS v(ccode, uname)
JOIN classes c ON c.code = v.ccode
JOIN users u  ON u.username = v.uname;

-- =====================================================================
-- 4. MODULE ĐỀ THI (exam) — BY_CLASS & SUPPLEMENTARY
-- =====================================================================
INSERT INTO exams (code, name, subject_id, type, duration_minutes, publish_at, end_at, status, created_at, created_by) VALUES
  ('DTO11-00001L', 'Kiểm tra giữa kỳ Toán 11',  6,  'BY_CLASS',      60,  TIMESTAMPTZ '2026-03-10 08:00:00+07', TIMESTAMPTZ '2026-03-10 09:00:00+07', 'ACTIVE', now(), 'seed-test'),
  ('DTO11-00001B', 'Đề bổ sung Toán 11',         6,  'SUPPLEMENTARY', 45,  TIMESTAMPTZ '2026-03-20 14:00:00+07', TIMESTAMPTZ '2026-03-20 14:45:00+07', 'ACTIVE', now(), 'seed-test'),
  ('DAN10-00001L', 'Final test English 10',      33, 'BY_CLASS',      90,  TIMESTAMPTZ '2026-05-15 09:00:00+07', TIMESTAMPTZ '2026-05-15 10:30:00+07', 'ACTIVE', now(), 'seed-test');

-- 4a. Đề ↔ Bài tập (+ điểm). TRUE_FALSE để points NULL (chấm theo từng ý ở 4b).
INSERT INTO exam_exercises (exam_id, exercise_id, sort_order, points)
SELECT ex.id, e.id, v.so, v.pts
FROM (VALUES
  ('DTO11-00001L','BTO11-00001N', 0, 3.00),
  ('DTO11-00001L','BTO11-00002N', 1, 3.00),
  ('DTO11-00001L','BTO11-00003L', 2, 2.00),
  ('DTO11-00001L','BTO11-00004D', 3, NULL),   -- TRUE_FALSE: điểm theo từng ý
  ('DTO11-00001B','BTO11-00001N', 0, 5.00),
  ('DTO11-00001B','BTO11-00003L', 1, 5.00),
  ('DAN10-00001L','BAN10-00001N', 0, 6.00),
  ('DAN10-00001L','BAN10-00002L', 1, 4.00)
) AS v(ecode, xcode, so, pts)
JOIN exams ex     ON ex.code = v.ecode
JOIN exercises e  ON e.code  = v.xcode;

-- 4b. Điểm từng ý đúng/sai cho bài BTO11-00004D trong đề DTO11-00001L (4 ý × 0.5 = 2.0)
INSERT INTO exam_tf_item_scores (exam_exercise_id, tf_item_id, points)
SELECT ee.id, tfi.id, 0.50
FROM exam_exercises ee
JOIN exams ex      ON ex.id = ee.exam_id      AND ex.code = 'DTO11-00001L'
JOIN exercises e   ON e.id  = ee.exercise_id  AND e.code  = 'BTO11-00004D'
JOIN true_false_items tfi ON tfi.exercise_id = e.id;

-- 4c. Đề ↔ Lớp áp dụng
INSERT INTO exam_classes (exam_id, class_id)
SELECT ex.id, c.id
FROM (VALUES
  ('DTO11-00001L','CTO11-00001L'),
  ('DTO11-00001B','CTO11-00001L'),
  ('DAN10-00001L','CAN10-00001L')
) AS v(ecode, ccode)
JOIN exams ex   ON ex.code = v.ecode
JOIN classes c  ON c.code  = v.ccode;

-- 4d. Đề bổ sung ↔ Học sinh cụ thể (chỉ SUPPLEMENTARY); HS phải thuộc lớp đã gán
INSERT INTO exam_students (exam_id, user_id)
SELECT ex.id, u.id
FROM (VALUES
  ('DTO11-00001B','hs01'),
  ('DTO11-00001B','hs02')
) AS v(ecode, uname)
JOIN exams ex  ON ex.code = v.ecode
JOIN users u   ON u.username = v.uname;

COMMIT;

-- ---------- Tổng kết ----------
SELECT 'users(test)'   AS bang, count(*) AS so_dong FROM users     WHERE created_by='seed-test'
UNION ALL SELECT 'exercises',     count(*) FROM exercises WHERE created_by='seed-test'
UNION ALL SELECT 'classes',       count(*) FROM classes   WHERE created_by='seed-test'
UNION ALL SELECT 'exams',         count(*) FROM exams     WHERE created_by='seed-test'
UNION ALL SELECT 'class_students',count(*) FROM class_students
UNION ALL SELECT 'exam_exercises',count(*) FROM exam_exercises
UNION ALL SELECT 'exam_tf_scores',count(*) FROM exam_tf_item_scores
UNION ALL SELECT 'exam_classes',  count(*) FROM exam_classes
UNION ALL SELECT 'exam_students', count(*) FROM exam_students
ORDER BY bang;
