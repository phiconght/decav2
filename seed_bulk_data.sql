-- =====================================================================
-- DỮ LIỆU TEST KHỐI LƯỢNG LỚN (bulk) cho DEV (PostgreSQL)
-- Sinh bằng generate_series. Đánh dấu created_by='seed-bulk' (độc lập với 'seed-test').
-- Re-runnable: tự xóa & tạo lại phần 'seed-bulk', KHÔNG đụng admin/seed-test/subjects.
-- Mật khẩu mọi user = Admin@123.
-- Chạy: psql -h localhost -U center -d center -v ON_ERROR_STOP=1 -f seed_bulk_data.sql
-- =====================================================================
\encoding UTF8
BEGIN;

\set pwd '''$2a$10$2SYKYkFDomaRDaR8b6QCGORR88hYA9IYGi4UY6C4EniNVfpCfSK4S'''

-- ---------- 0. Dọn dữ liệu bulk cũ (đúng thứ tự FK) ----------
DELETE FROM exams      WHERE created_by = 'seed-bulk';
DELETE FROM classes    WHERE created_by = 'seed-bulk';
DELETE FROM exercises  WHERE created_by = 'seed-bulk';
DELETE FROM users      WHERE created_by = 'seed-bulk';

-- =====================================================================
-- 1. USERS — 200 HS, 40 GV, 20 PH, 10 NV, 5 trợ giảng
--    full_name ghép từ mảng họ + đệm + tên (đa dạng theo modulo)
-- =====================================================================
-- Học sinh (bs0001..bs0200): đa số ACTIVE, rải rác DISABLED/LOCKED
INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by)
SELECT 'bs'||lpad(n::text,4,'0'),
       'bs'||lpad(n::text,4,'0')||'@trungtam.vn',
       '093'||lpad(n::text,7,'0'),
       :pwd,
       (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Huỳnh','Phan','Vũ','Võ','Đặng','Bùi','Đỗ','Hồ','Ngô','Dương','Lý'])[(n%16)+1]
         ||' '|| (ARRAY['Văn','Thị','Hữu','Đức','Minh','Quốc','Thanh','Hồng','Ngọc','Gia'])[(n%10)+1]
         ||' '|| (ARRAY['An','Bình','Chi','Dũng','Phúc','Giang','Hà','Khôi','Lan','Mai','Nam','Oanh','Phong','Quân','Sơn','Trang','Uyên','Vy','Yến','Tú'])[(n%20)+1],
       CASE WHEN n%25=0 THEN 'LOCKED' WHEN n%20=0 THEN 'DISABLED' ELSE 'ACTIVE' END,
       now(), 'seed-bulk'
FROM generate_series(1,200) n;

-- Giáo viên (bt001..bt040)
INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by)
SELECT 'bt'||lpad(n::text,3,'0'),
       'bt'||lpad(n::text,3,'0')||'@trungtam.vn',
       '091'||lpad(n::text,7,'0'),
       :pwd,
       'GV '||(ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Phan','Vũ','Đặng','Bùi','Ngô'])[(n%10)+1]
         ||' '|| (ARRAY['Văn','Thị','Đức','Minh','Quốc','Thanh'])[(n%6)+1]
         ||' '|| (ARRAY['Hùng','Lan','Sơn','Mai','Tâm','Phúc','Nga','Dũng'])[(n%8)+1],
       'ACTIVE', now(), 'seed-bulk'
FROM generate_series(1,40) n;

-- Phụ huynh (bp001..bp020)
INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by)
SELECT 'bp'||lpad(n::text,3,'0'),
       'bp'||lpad(n::text,3,'0')||'@trungtam.vn',
       '094'||lpad(n::text,7,'0'),
       :pwd,
       'PH '||(ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng'])[(n%5)+1]
         ||' '|| (ARRAY['Văn','Thị','Hữu'])[(n%3)+1]
         ||' '|| (ARRAY['Bình','Hoa','Long','Thu','Quang'])[(n%5)+1],
       CASE WHEN n%15=0 THEN 'DISABLED' ELSE 'ACTIVE' END,
       now(), 'seed-bulk'
FROM generate_series(1,20) n;

-- Nhân viên (be001..be010)
INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by)
SELECT 'be'||lpad(n::text,3,'0'),
       'be'||lpad(n::text,3,'0')||'@trungtam.vn',
       '092'||lpad(n::text,7,'0'),
       :pwd,
       'NV '||(ARRAY['Phạm','Lê','Trần','Vũ','Đỗ'])[(n%5)+1]
         ||' Thị '|| (ARRAY['Hoa','Lan','Hương','Trang','Yến'])[(n%5)+1],
       'ACTIVE', now(), 'seed-bulk'
FROM generate_series(1,10) n;

-- Trợ giảng (ba001..ba005)
INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by)
SELECT 'ba'||lpad(n::text,3,'0'),
       'ba'||lpad(n::text,3,'0')||'@trungtam.vn',
       '095'||lpad(n::text,7,'0'),
       :pwd,
       'TG '||(ARRAY['Ngô','Bùi','Đặng','Hồ','Dương'])[(n%5)+1]
         ||' Văn '|| (ARRAY['Khoa','Tài','Đạt','Huy','Phong'])[(n%5)+1],
       'ACTIVE', now(), 'seed-bulk'
FROM generate_series(1,5) n;

-- Gán vai trò theo tiền tố username
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = CASE substring(u.username,1,2)
                            WHEN 'bs' THEN 'STUDENT'
                            WHEN 'bt' THEN 'TEACHER'
                            WHEN 'bp' THEN 'PARENT'
                            WHEN 'be' THEN 'EMPLOYEE'
                            WHEN 'ba' THEN 'ASSISTANT'
                         END
WHERE u.created_by = 'seed-bulk';

-- =====================================================================
-- 2. EXERCISES — 120 bài, rải đều 49 môn, xoay vòng 3 loại
--    subject_id = (n%49)+1  (subjects có id 1..49)
-- =====================================================================
INSERT INTO exercises (code, title, subject_id, type, status, question_text, essay_answer, created_at, created_by)
SELECT 'BLKB-'||lpad(n::text,5,'0'),
       'Bài tập bulk số '||n,
       (n%49)+1,
       (ARRAY['MULTIPLE_CHOICE','ESSAY','TRUE_FALSE'])[(n%3)+1],
       CASE WHEN n%15=0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
       'Nội dung câu hỏi của bài tập bulk số '||n||'. Đây là dữ liệu test sinh tự động.',
       CASE WHEN (n%3)+1 = 2 THEN 'Đáp án mẫu (tự luận) cho bài bulk số '||n ELSE NULL END,
       now(), 'seed-bulk'
FROM generate_series(1,120) n;

-- 2a. Đáp án trắc nghiệm: mỗi bài 4 phương án, đúng 1 đáp án đúng (theo id%4)
INSERT INTO choice_options (exercise_id, sort_order, text, is_correct)
SELECT e.id, opt, 'Phương án '||(opt+1)||' của '||e.code, (opt = (e.id % 4))
FROM exercises e
CROSS JOIN generate_series(0,3) opt
WHERE e.created_by='seed-bulk' AND e.type='MULTIPLE_CHOICE';

-- 2b. Ý đúng/sai: mỗi bài 3 ý, answer xen kẽ
INSERT INTO true_false_items (exercise_id, sort_order, text, answer)
SELECT e.id, it, 'Ý số '||(it+1)||' của '||e.code, (((e.id+it)%2)=0)
FROM exercises e
CROSS JOIN generate_series(0,2) it
WHERE e.created_by='seed-bulk' AND e.type='TRUE_FALSE';

-- =====================================================================
-- 3. CLASSES — 40 lớp + ghi danh học sinh
-- =====================================================================
INSERT INTO classes (code, name, subject_id, start_date, end_date, status, created_at, created_by)
SELECT 'BLKC-'||lpad(n::text,5,'0'),
       'Lớp bulk '||n,
       (n%49)+1,
       DATE '2026-01-05' + (n*2),
       DATE '2026-06-05' + (n*2),
       CASE WHEN n%10=0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
       now(), 'seed-bulk'
FROM generate_series(1,40) n;

-- Ghi danh: HS bulk vào lớp bulk theo cùng số dư (mỗi lớp ~15 HS; 1 HS có thể nhiều lớp)
WITH s AS (
  SELECT id, row_number() OVER (ORDER BY id) rn
  FROM users WHERE created_by='seed-bulk' AND username LIKE 'bs%'
), c AS (
  SELECT id, row_number() OVER (ORDER BY id) rn
  FROM classes WHERE created_by='seed-bulk'
)
INSERT INTO class_students (class_id, user_id)
SELECT c.id, s.id
FROM c JOIN s ON (s.rn % 13) = (c.rn % 13);

-- =====================================================================
-- 4. EXAMS — 40 đề (3/4 BY_CLASS, 1/4 SUPPLEMENTARY)
-- =====================================================================
INSERT INTO exams (code, name, subject_id, type, duration_minutes, publish_at, end_at, status, created_at, created_by)
SELECT 'BLKD-'||lpad(n::text,5,'0'),
       'Đề thi bulk số '||n,
       (n%49)+1,
       CASE WHEN n%4=0 THEN 'SUPPLEMENTARY' ELSE 'BY_CLASS' END,
       (ARRAY[45,60,90,120])[(n%4)+1],
       TIMESTAMPTZ '2026-04-01 08:00:00+07' + (n||' days')::interval,
       TIMESTAMPTZ '2026-04-01 09:30:00+07' + (n||' days')::interval,
       CASE WHEN n%12=0 THEN 'INACTIVE' ELSE 'ACTIVE' END,
       now(), 'seed-bulk'
FROM generate_series(1,40) n;

-- 4a. Đề ↔ Bài tập: mỗi đề 2 bài (không phải TRUE_FALSE), điểm 5+5 = 10
WITH ex AS (
  SELECT id, row_number() OVER (ORDER BY id) rn FROM exams WHERE created_by='seed-bulk'
), pool AS (
  SELECT id, row_number() OVER (ORDER BY id) rn
  FROM exercises WHERE created_by='seed-bulk' AND type <> 'TRUE_FALSE'
), poolsize AS (
  SELECT count(*) c FROM exercises WHERE created_by='seed-bulk' AND type <> 'TRUE_FALSE'
)
INSERT INTO exam_exercises (exam_id, exercise_id, sort_order, points)
SELECT ex.id, pool.id, k, 5.00
FROM ex
CROSS JOIN generate_series(0,1) k
CROSS JOIN poolsize
JOIN pool ON pool.rn = ((ex.rn*2 + k) % poolsize.c) + 1;

-- 4b. Đề ↔ Lớp: mỗi đề gán đúng 1 lớp bulk (ánh xạ 1:1 theo thứ tự)
WITH ex AS (
  SELECT id, row_number() OVER (ORDER BY id) rn FROM exams WHERE created_by='seed-bulk'
), c AS (
  SELECT id, row_number() OVER (ORDER BY id) rn FROM classes WHERE created_by='seed-bulk'
)
INSERT INTO exam_classes (exam_id, class_id)
SELECT ex.id, c.id
FROM ex JOIN c ON c.rn = ((ex.rn - 1) % 40) + 1;

-- 4c. Đề bổ sung ↔ HS: lấy HS đã ghi danh trong lớp đã gán (đảm bảo HS ⊆ lớp của đề)
INSERT INTO exam_students (exam_id, user_id)
SELECT ec.exam_id, cs.user_id
FROM exam_classes ec
JOIN exams ex ON ex.id = ec.exam_id AND ex.created_by='seed-bulk' AND ex.type='SUPPLEMENTARY'
JOIN class_students cs ON cs.class_id = ec.class_id;

COMMIT;

-- ---------- Tổng kết bulk ----------
SELECT 'users'          bang, count(*) n FROM users          WHERE created_by='seed-bulk'
UNION ALL SELECT 'exercises',      count(*) FROM exercises    WHERE created_by='seed-bulk'
UNION ALL SELECT 'classes',        count(*) FROM classes      WHERE created_by='seed-bulk'
UNION ALL SELECT 'exams',          count(*) FROM exams        WHERE created_by='seed-bulk'
UNION ALL SELECT 'class_students', count(*) FROM class_students cs WHERE EXISTS(SELECT 1 FROM classes c WHERE c.id=cs.class_id AND c.created_by='seed-bulk')
UNION ALL SELECT 'exam_exercises', count(*) FROM exam_exercises ee WHERE EXISTS(SELECT 1 FROM exams e WHERE e.id=ee.exam_id AND e.created_by='seed-bulk')
UNION ALL SELECT 'exam_classes',   count(*) FROM exam_classes ec WHERE EXISTS(SELECT 1 FROM exams e WHERE e.id=ec.exam_id AND e.created_by='seed-bulk')
UNION ALL SELECT 'exam_students',  count(*) FROM exam_students es WHERE EXISTS(SELECT 1 FROM exams e WHERE e.id=es.exam_id AND e.created_by='seed-bulk')
ORDER BY bang;
