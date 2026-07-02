-- =====================================================================
-- SEED FULL-FLOW (seed-full) — dữ liệu test toàn luồng, liên kết chặt.
-- Phủ: users+roles+PH, exercises(+topic+difficulty+options/tf), classes(+giá),
--   ghi danh(+giảm giá), giáo viên, buổi học+điểm danh, đề thi(+chương)+bài,
--   exam_student(DA_LAM...)+kết quả từng câu(score coherent), nhận xét,
--   giao bài luyện tập, hóa đơn học phí(+items), ví Xu+giao dịch.
-- Idempotent: xóa & tạo lại phần seed-full. Mật khẩu mọi user = Admin@123.
-- Chạy: psql -h localhost -U center -d center -v ON_ERROR_STOP=1 -f seed_fullflow.sql
-- =====================================================================
\encoding UTF8
BEGIN;
\set pwd '''$2a$10$2SYKYkFDomaRDaR8b6QCGORR88hYA9IYGi4UY6C4EniNVfpCfSK4S'''

-- ---------- 0. Dọn seed-full cũ (đúng thứ tự FK) ----------
DELETE FROM coin_transactions      WHERE created_by='seed-full';
DELETE FROM coin_wallets           WHERE created_by='seed-full';
DELETE FROM tuition_invoice_items  WHERE created_by='seed-full';
DELETE FROM tuition_invoices       WHERE created_by='seed-full';
DELETE FROM practice_assignments   WHERE created_by='seed-full';
DELETE FROM report_comments        WHERE created_by='seed-full';
DELETE FROM exam_question_result   WHERE created_by='seed-full';
DELETE FROM session_attendance     WHERE created_by='seed-full';
DELETE FROM class_sessions         WHERE created_by='seed-full';
DELETE FROM exam_student           WHERE created_by='seed-full';
DELETE FROM exams                  WHERE created_by='seed-full';  -- cascade exam_exercises/classes/students
DELETE FROM classes                WHERE created_by='seed-full';  -- cascade class_students/teachers
DELETE FROM exercises              WHERE created_by='seed-full';
DELETE FROM student_parents        WHERE created_by='seed-full';
DELETE FROM users                  WHERE created_by='seed-full';  -- cascade user_roles

-- =====================================================================
-- 1. USERS: 150 HS, 25 GV, 60 PH, 12 NV, 8 TG
-- =====================================================================
INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by)
SELECT 'fs'||lpad(n::text,4,'0'), 'fs'||lpad(n::text,4,'0')||'@tt.vn', '093'||lpad(n::text,7,'0'), :pwd,
   (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Huỳnh','Phan','Vũ','Võ','Đặng','Bùi','Đỗ','Hồ','Ngô','Dương','Lý'])[(n%16)+1]
     ||' '||(ARRAY['Văn','Thị','Hữu','Đức','Minh','Quốc','Thanh','Hồng','Ngọc','Gia'])[(n%10)+1]
     ||' '||(ARRAY['An','Bình','Chi','Dũng','Phúc','Giang','Hà','Khôi','Lan','Mai','Nam','Oanh','Phong','Quân','Sơn','Trang','Uyên','Vy','Yến','Tú'])[(n%20)+1],
   CASE WHEN n%30=0 THEN 'DISABLED' ELSE 'ACTIVE' END, now(), 'seed-full'
FROM generate_series(1,150) n;

INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by)
SELECT 'ft'||lpad(n::text,3,'0'), 'ft'||lpad(n::text,3,'0')||'@tt.vn', '091'||lpad(n::text,7,'0'), :pwd,
   'GV '||(ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Phan','Vũ','Đặng','Bùi','Ngô'])[(n%10)+1]
     ||' '||(ARRAY['Văn','Thị','Đức','Minh','Quốc','Thanh'])[(n%6)+1]
     ||' '||(ARRAY['Hùng','Lan','Sơn','Mai','Tâm','Phúc','Nga','Dũng'])[(n%8)+1],
   'ACTIVE', now(), 'seed-full'
FROM generate_series(1,25) n;

INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by)
SELECT 'fp'||lpad(n::text,3,'0'), 'fp'||lpad(n::text,3,'0')||'@tt.vn', '094'||lpad(n::text,7,'0'), :pwd,
   'PH '||(ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Vũ','Đỗ'])[(n%7)+1]
     ||' '||(ARRAY['Văn','Thị','Hữu'])[(n%3)+1]
     ||' '||(ARRAY['Bình','Hoa','Long','Thu','Quang','Hương','Tuấn'])[(n%7)+1],
   'ACTIVE', now(), 'seed-full'
FROM generate_series(1,60) n;

INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by)
SELECT 'fe'||lpad(n::text,3,'0'), 'fe'||lpad(n::text,3,'0')||'@tt.vn', '092'||lpad(n::text,7,'0'), :pwd,
   'NV '||(ARRAY['Phạm','Lê','Trần','Vũ','Đỗ'])[(n%5)+1]||' Thị '||(ARRAY['Hoa','Lan','Hương','Trang','Yến'])[(n%5)+1],
   'ACTIVE', now(), 'seed-full'
FROM generate_series(1,12) n;

INSERT INTO users (username, email, phone, password_hash, full_name, status, created_at, created_by)
SELECT 'fa'||lpad(n::text,3,'0'), 'fa'||lpad(n::text,3,'0')||'@tt.vn', '095'||lpad(n::text,7,'0'), :pwd,
   'TG '||(ARRAY['Ngô','Bùi','Đặng','Hồ','Dương'])[(n%5)+1]||' Văn '||(ARRAY['Khoa','Tài','Đạt','Huy','Phong'])[(n%5)+1],
   'ACTIVE', now(), 'seed-full'
FROM generate_series(1,8) n;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u
JOIN roles r ON r.name = CASE substring(u.username,1,2)
   WHEN 'fs' THEN 'STUDENT' WHEN 'ft' THEN 'TEACHER' WHEN 'fp' THEN 'PARENT'
   WHEN 'fe' THEN 'EMPLOYEE' WHEN 'fa' THEN 'ASSISTANT' END
WHERE u.created_by='seed-full';

-- 1b. Liên kết PH–HS: mỗi HS 1 PH (xoay 60 PH), 1/4 HS có thêm PH thứ 2
WITH s AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM users WHERE created_by='seed-full' AND username LIKE 'fs%'),
     p AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM users WHERE created_by='seed-full' AND username LIKE 'fp%')
INSERT INTO student_parents (student_id, parent_id, relationship, created_at, created_by)
SELECT s.id, p.id, (ARRAY['FATHER','MOTHER','GUARDIAN'])[(s.rn%3)+1], now(), 'seed-full'
FROM s JOIN p ON p.rn = ((s.rn-1)%60)+1;
WITH s AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM users WHERE created_by='seed-full' AND username LIKE 'fs%'),
     p AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM users WHERE created_by='seed-full' AND username LIKE 'fp%')
INSERT INTO student_parents (student_id, parent_id, relationship, created_at, created_by)
SELECT s.id, p.id, 'MOTHER', now(), 'seed-full'
FROM s JOIN p ON p.rn = ((s.rn+29)%60)+1
WHERE s.rn%4=0;

-- =====================================================================
-- 2. EXERCISES: mỗi chuyên đề (subject 1..20) 18 bài, rải độ khó+loại
-- =====================================================================
INSERT INTO exercises (code, title, subject_id, topic_id, type, difficulty, status, question_text, essay_answer, created_at, created_by)
SELECT 'FSX-'||lpad(t.id::text,4,'0')||'-'||lpad(k::text,2,'0'),
   'Bài '||k||' — '||t.name,
   t.subject_id, t.id,
   (ARRAY['MULTIPLE_CHOICE','MULTIPLE_CHOICE','MULTIPLE_CHOICE','TRUE_FALSE','TRUE_FALSE','ESSAY'])[(k%6)+1],
   (ARRAY['EASY','EASY','MEDIUM','MEDIUM','MEDIUM','HARD'])[(k%6)+1],
   'ACTIVE',
   'Câu hỏi bài '||k||' thuộc chuyên đề "'||t.name||'". Dữ liệu test seed-full.',
   CASE WHEN (k%6)+1=6 THEN 'Đáp án mẫu tự luận bài '||k||' ('||t.name||')' ELSE NULL END,
   now(), 'seed-full'
FROM topics t CROSS JOIN generate_series(1,18) k
WHERE t.subject_id <= 20;

-- 2a. MC: 4 phương án, đúng 1 (theo id%4)
INSERT INTO choice_options (exercise_id, sort_order, text, is_correct)
SELECT e.id, opt, 'Phương án '||(opt+1), (opt = (e.id%4))
FROM exercises e CROSS JOIN generate_series(0,3) opt
WHERE e.created_by='seed-full' AND e.type='MULTIPLE_CHOICE';

-- 2b. TF: 3 ý
INSERT INTO true_false_items (exercise_id, sort_order, text, answer)
SELECT e.id, it, 'Ý '||(it+1), (((e.id+it)%2)=0)
FROM exercises e CROSS JOIN generate_series(0,2) it
WHERE e.created_by='seed-full' AND e.type='TRUE_FALSE';

-- =====================================================================
-- 3. CLASSES: 24 lớp (subject 1..20 xoay vòng), có giá + GV
-- =====================================================================
INSERT INTO classes (code, name, subject_id, start_date, end_date, status, price_per_session, created_at, created_by)
SELECT 'FSC-'||lpad(n::text,3,'0'),
   'Lớp '||(ARRAY['Toán','Lý','Hóa'])[(((n-1)%20)/7)+1]||' '||n,
   ((n-1)%20)+1,
   DATE '2026-01-05', DATE '2026-06-27',
   'ACTIVE',
   (ARRAY[150000,180000,200000,220000,250000,300000])[(n%6)+1],
   now(), 'seed-full'
FROM generate_series(1,24) n;

-- GV: mỗi lớp 1 GV chính; lớp chẵn thêm GV phụ
WITH c AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM classes WHERE created_by='seed-full'),
     t AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM users WHERE created_by='seed-full' AND username LIKE 'ft%')
INSERT INTO class_teachers (class_id, user_id)
SELECT c.id, t.id FROM c JOIN t ON t.rn = ((c.rn-1)%25)+1;
WITH c AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM classes WHERE created_by='seed-full'),
     t AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM users WHERE created_by='seed-full' AND username LIKE 'ft%')
INSERT INTO class_teachers (class_id, user_id)
SELECT c.id, t.id FROM c JOIN t ON t.rn = ((c.rn+11)%25)+1 WHERE c.rn%2=0;

-- Ghi danh: HS residue theo rn%6 = lớp rn%6 (~25 HS/lớp, HS học nhiều lớp) + giảm giá
WITH s AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM users WHERE created_by='seed-full' AND username LIKE 'fs%'),
     c AS (SELECT id, row_number() OVER (ORDER BY id) rn FROM classes WHERE created_by='seed-full')
INSERT INTO class_students (class_id, user_id, enrolled_at, discount_percent)
SELECT c.id, s.id, DATE '2026-01-05',
   (ARRAY[0,0,0,0,0,10,10,20,20,50,100])[(s.rn%11)+1]
FROM c JOIN s ON (s.rn%6) = (c.rn%6);

-- =====================================================================
-- 4. BUỔI HỌC: 16 buổi/lớp (T2 hàng tuần từ 05/01/2026), đều DONE (đã qua)
-- =====================================================================
INSERT INTO class_sessions (class_id, session_date, start_time, duration_minutes, teacher_id, status, price, price_overridden, is_manual, created_at, created_by)
SELECT c.id,
   DATE '2026-01-05' + (w*7),
   (ARRAY[TIME '18:00', TIME '19:30', TIME '08:00'])[(c.rn%3)+1],
   90,
   (SELECT ct.user_id FROM class_teachers ct WHERE ct.class_id=c.id LIMIT 1),
   'DONE',
   c.price_per_session, false, false, now(), 'seed-full'
FROM (SELECT id, price_per_session, row_number() OVER (ORDER BY id) rn FROM classes WHERE created_by='seed-full') c
CROSS JOIN generate_series(0,15) w;

-- Điểm danh: mỗi buổi DONE × HS ghi danh; phân bố trạng thái
INSERT INTO session_attendance (session_id, user_id, status, check_in_at, created_at, created_by)
SELECT ses.id, cs.user_id,
   (ARRAY['CO_MAT','CO_MAT','CO_MAT','CO_MAT','CO_MAT','CO_MAT','TRE','TRE','VANG','CO_PHEP','CHUA_CHECKIN'])[((cs.user_id + ses.id)%11)+1],
   CASE WHEN ((cs.user_id+ses.id)%11) < 8 THEN (ses.session_date + ses.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh' ELSE NULL END,
   now(), 'seed-full'
FROM class_sessions ses
JOIN classes c ON c.id=ses.class_id AND c.created_by='seed-full'
JOIN class_students cs ON cs.class_id=ses.class_id
WHERE ses.created_by='seed-full';

-- =====================================================================
-- 5. ĐỀ THI: 4 đề BY_CLASS / lớp, có chương (topic của môn lớp)
-- =====================================================================
INSERT INTO exams (code, name, subject_id, topic_id, type, duration_minutes, publish_at, end_at, status, created_at, created_by)
SELECT 'FSD-'||lpad(c.rn::text,3,'0')||'-'||k,
   'KT '||k||' — lớp '||c.rn,
   c.subject_id,
   (SELECT t.id FROM topics t WHERE t.subject_id=c.subject_id ORDER BY t.sort_order OFFSET ((k-1) % GREATEST(1,(SELECT count(*) FROM topics t2 WHERE t2.subject_id=c.subject_id))) LIMIT 1),
   'BY_CLASS',
   60,
   (TIMESTAMPTZ '2026-02-01 08:00:00+07' + ((c.rn*4+k)||' days')::interval),
   (TIMESTAMPTZ '2026-02-01 09:00:00+07' + ((c.rn*4+k)||' days')::interval),
   'ACTIVE', now(), 'seed-full'
FROM (SELECT id, subject_id, row_number() OVER (ORDER BY id) rn FROM classes WHERE created_by='seed-full') c
CROSS JOIN generate_series(1,4) k;

-- 5a. Đề ↔ lớp (FSD-{classRn}-k → lớp classRn)
INSERT INTO exam_classes (exam_id, class_id)
SELECT e.id, c.id
FROM exams e
JOIN (SELECT id, row_number() OVER (ORDER BY id) rn FROM classes WHERE created_by='seed-full') c
  ON e.code LIKE 'FSD-'||lpad(c.rn::text,3,'0')||'-%'
WHERE e.created_by='seed-full';

-- 5b. Đề ↔ bài: 10 bài của ĐÚNG chương của đề (điểm 1 mỗi câu → max 10)
WITH ranked AS (
  SELECT e.id exam_id, ex.id ex_id,
     row_number() OVER (PARTITION BY e.id ORDER BY (ex.id + e.id)) rn
  FROM exams e
  JOIN exercises ex ON ex.topic_id = e.topic_id AND ex.created_by='seed-full' AND ex.status='ACTIVE'
  WHERE e.created_by='seed-full'
)
INSERT INTO exam_exercises (exam_id, exercise_id, sort_order, points)
SELECT exam_id, ex_id, rn-1, 1.00 FROM ranked WHERE rn <= 10;

-- =====================================================================
-- 6. EXAM_STUDENT: mỗi đề × HS của lớp; 72% DA_LAM
-- =====================================================================
WITH es_plan AS (
  SELECT ec.exam_id, cs.user_id,
    CASE WHEN (cs.user_id*3 + ec.exam_id) % 100 < 72 THEN 'DA_LAM'
         WHEN (cs.user_id*3 + ec.exam_id) % 100 < 88 THEN 'DA_PHAT_HANH'
         ELSE 'CHUA_PHAT_HANH' END AS st
  FROM exam_classes ec
  JOIN exams e ON e.id=ec.exam_id AND e.created_by='seed-full'
  JOIN class_students cs ON cs.class_id=ec.class_id
)
INSERT INTO exam_student (exam_id, user_id, source, status, started_at, submitted_at, answers, created_at, created_by)
SELECT exam_id, user_id, 'CLASS', st,
   CASE WHEN st='DA_LAM' THEN now()-interval '20 days' ELSE NULL END,
   CASE WHEN st='DA_LAM' THEN now()-interval '20 days' ELSE NULL END,
   CASE WHEN st='DA_LAM' THEN '{"mc":{},"tf":{},"essay":{}}' ELSE NULL END,
   now(), 'seed-full'
FROM es_plan;

-- 6a. Kết quả từng câu: correct theo "năng lực" HV (ổn định) → điểm rải thực
INSERT INTO exam_question_result (exam_student_id, exam_exercise_id, earned, max_points, correct, created_at, created_by)
SELECT es.id, ee.id,
   CASE WHEN ex.type='ESSAY' THEN 0
        WHEN ((es.user_id*7 + ee.id*13 + es.exam_id) % 100) < (45 + (es.user_id % 50)) THEN 1 ELSE 0 END,
   1.00,
   CASE WHEN ex.type='ESSAY' THEN NULL
        WHEN ((es.user_id*7 + ee.id*13 + es.exam_id) % 100) < (45 + (es.user_id % 50)) THEN true ELSE false END,
   now(), 'seed-full'
FROM exam_student es
JOIN exam_exercises ee ON ee.exam_id = es.exam_id
JOIN exercises ex ON ex.id = ee.exercise_id
WHERE es.created_by='seed-full' AND es.status='DA_LAM';

-- 6b. Điểm = tổng earned
UPDATE exam_student es
SET score = COALESCE((SELECT SUM(r.earned) FROM exam_question_result r WHERE r.exam_student_id=es.id),0)
WHERE es.created_by='seed-full' AND es.status='DA_LAM';

-- =====================================================================
-- 7. NHẬN XÉT: GV nhận xét ~1/3 (HS,lớp); một số cho HS xem
-- =====================================================================
WITH pairs AS (
  SELECT DISTINCT cs.user_id student_id, cs.class_id,
     (SELECT ct.user_id FROM class_teachers ct WHERE ct.class_id=cs.class_id LIMIT 1) teacher_id,
     row_number() OVER (ORDER BY cs.class_id, cs.user_id) rn
  FROM class_students cs JOIN classes c ON c.id=cs.class_id AND c.created_by='seed-full'
)
INSERT INTO report_comments (student_id, class_id, author_id, author_role, content, visible_to_student, created_at, created_by)
SELECT student_id, class_id, teacher_id, 'TEACHER',
   (ARRAY['Em tiến bộ tốt, cần luyện thêm phần khó.','Chăm chỉ nhưng hay sai câu dễ do bất cẩn.','Nắm vững lý thuyết, cần tăng tốc độ làm bài.','Kết quả ổn định, giữ phong độ nhé.','Cần ôn lại chương gần nhất, còn hổng kiến thức.'])[(rn%5)+1],
   (rn%3=0), now()-interval '5 days', 'seed-full'
FROM pairs WHERE rn%3=0 AND teacher_id IS NOT NULL;

-- =====================================================================
-- 8. GIAO BÀI LUYỆN TẬP: 40 đề SUPPLEMENTARY (PH giao)
-- =====================================================================
WITH pairs AS (
  SELECT cs.user_id student_id, cs.class_id, c.subject_id,
     (SELECT sp.parent_id FROM student_parents sp WHERE sp.student_id=cs.user_id AND sp.created_by='seed-full' LIMIT 1) parent_id,
     row_number() OVER (ORDER BY cs.class_id, cs.user_id) rn
  FROM class_students cs JOIN classes c ON c.id=cs.class_id AND c.created_by='seed-full'
  WHERE EXISTS (SELECT 1 FROM student_parents sp WHERE sp.student_id=cs.user_id AND sp.created_by='seed-full')
)
INSERT INTO exams (code, name, subject_id, topic_id, type, duration_minutes, publish_at, end_at, status, created_at, created_by)
SELECT 'FSPR-'||lpad(p.rn::text,4,'0'), 'Luyện tập PH giao — '||t.name, p.subject_id, t.id,
   'SUPPLEMENTARY', 30, now(), now()+interval '7 days', 'ACTIVE', now(), 'seed-full'
FROM pairs p
JOIN LATERAL (SELECT id,name FROM topics WHERE subject_id=p.subject_id ORDER BY sort_order LIMIT 1) t ON true
WHERE p.rn <= 40 AND p.parent_id IS NOT NULL;

-- 8a. Bài cho đề luyện (10 câu của chương)
WITH ranked AS (
  SELECT e.id exam_id, ex.id ex_id, row_number() OVER (PARTITION BY e.id ORDER BY ex.id) rn
  FROM exams e JOIN exercises ex ON ex.topic_id=e.topic_id AND ex.created_by='seed-full' AND ex.status='ACTIVE'
  WHERE e.created_by='seed-full' AND e.code LIKE 'FSPR-%'
)
INSERT INTO exam_exercises (exam_id, exercise_id, sort_order, points)
SELECT exam_id, ex_id, rn-1, 1.00 FROM ranked WHERE rn<=10;

-- 8b. exam_student cho HS được giao
WITH pairs AS (
  SELECT cs.user_id student_id, cs.class_id,
     (SELECT sp.parent_id FROM student_parents sp WHERE sp.student_id=cs.user_id AND sp.created_by='seed-full' LIMIT 1) parent_id,
     row_number() OVER (ORDER BY cs.class_id, cs.user_id) rn
  FROM class_students cs JOIN classes c ON c.id=cs.class_id AND c.created_by='seed-full'
  WHERE EXISTS (SELECT 1 FROM student_parents sp WHERE sp.student_id=cs.user_id AND sp.created_by='seed-full')
)
INSERT INTO exam_student (exam_id, user_id, source, status, created_at, created_by)
SELECT e.id, p.student_id, 'SUPPLEMENTARY', 'CHUA_PHAT_HANH', now(), 'seed-full'
FROM pairs p JOIN exams e ON e.code='FSPR-'||lpad(p.rn::text,4,'0') AND e.created_by='seed-full'
WHERE p.rn<=40 AND p.parent_id IS NOT NULL;

-- 8c. practice_assignments
WITH pairs AS (
  SELECT cs.user_id student_id, cs.class_id,
     (SELECT sp.parent_id FROM student_parents sp WHERE sp.student_id=cs.user_id AND sp.created_by='seed-full' LIMIT 1) parent_id,
     row_number() OVER (ORDER BY cs.class_id, cs.user_id) rn
  FROM class_students cs JOIN classes c ON c.id=cs.class_id AND c.created_by='seed-full'
  WHERE EXISTS (SELECT 1 FROM student_parents sp WHERE sp.student_id=cs.user_id AND sp.created_by='seed-full')
)
INSERT INTO practice_assignments (exam_id, source_exam_id, student_id, parent_id, class_id, status, created_at, created_by)
SELECT e.id,
   (SELECT ec.exam_id FROM exam_classes ec JOIN exams se ON se.id=ec.exam_id AND se.type='BY_CLASS' WHERE ec.class_id=p.class_id LIMIT 1),
   p.student_id, p.parent_id, p.class_id, 'ASSIGNED', now(), 'seed-full'
FROM pairs p JOIN exams e ON e.code='FSPR-'||lpad(p.rn::text,4,'0') AND e.created_by='seed-full'
WHERE p.rn<=40 AND p.parent_id IS NOT NULL;

-- =====================================================================
-- 9. HỌC PHÍ: 1 đợt thu / (HS,lớp) theo buổi tính phí (bỏ CO_PHEP)
-- =====================================================================
WITH billable AS (
  SELECT ses.class_id, sa.user_id, ses.id session_id, ses.session_date, ses.price
  FROM session_attendance sa
  JOIN class_sessions ses ON ses.id=sa.session_id AND ses.created_by='seed-full' AND ses.status='DONE'
  WHERE sa.created_by='seed-full' AND sa.status <> 'CO_PHEP'
),
agg AS (
  SELECT b.class_id, b.user_id, count(*) cnt, sum(b.price) gross,
     COALESCE((SELECT discount_percent FROM class_students cs WHERE cs.class_id=b.class_id AND cs.user_id=b.user_id),0) disc
  FROM billable b GROUP BY b.class_id, b.user_id
)
INSERT INTO tuition_invoices (student_id, class_id, period_from, period_to, session_count, gross_amount, discount_percent, amount, payment_code, status, confirmed_at, paid_at, created_at, created_by)
SELECT a.user_id, a.class_id, DATE '2026-01-05', DATE '2026-06-27', a.cnt, a.gross, a.disc,
   floor(a.gross*(100-a.disc)/100/1000)*1000,
   u.username||'-'||upper(substring(md5(a.user_id::text||'-'||a.class_id::text) for 4)),
   (ARRAY['DRAFT','DRAFT','CONFIRMED','CONFIRMED','PAID'])[((a.user_id+a.class_id)%5)+1],
   CASE WHEN ((a.user_id+a.class_id)%5)>=2 THEN now()-interval '3 days' ELSE NULL END,
   CASE WHEN ((a.user_id+a.class_id)%5)=4 THEN now()-interval '1 days' ELSE NULL END,
   now(), 'seed-full'
FROM agg a JOIN users u ON u.id=a.user_id
WHERE a.cnt > 0;

-- 9a. Items (từng buổi tính phí)
WITH billable AS (
  SELECT ses.class_id, sa.user_id, ses.id session_id, ses.session_date, ses.price
  FROM session_attendance sa
  JOIN class_sessions ses ON ses.id=sa.session_id AND ses.created_by='seed-full' AND ses.status='DONE'
  WHERE sa.created_by='seed-full' AND sa.status <> 'CO_PHEP'
)
INSERT INTO tuition_invoice_items (invoice_id, session_id, session_date, price, created_at, created_by)
SELECT inv.id, b.session_id, b.session_date, b.price, now(), 'seed-full'
FROM billable b
JOIN tuition_invoices inv ON inv.class_id=b.class_id AND inv.student_id=b.user_id AND inv.created_by='seed-full';

-- =====================================================================
-- 10. XU: ví + 2 giao dịch / HS
-- =====================================================================
WITH s AS (SELECT id, (50 + (id%40)*5) AS bal FROM users WHERE created_by='seed-full' AND username LIKE 'fs%')
INSERT INTO coin_wallets (user_id, balance, created_at, created_by)
SELECT id, bal, now(), 'seed-full' FROM s;

WITH s AS (SELECT id, (50 + (id%40)*5) AS bal FROM users WHERE created_by='seed-full' AND username LIKE 'fs%')
INSERT INTO coin_transactions (user_id, amount, balance_after, reason, created_at, created_by)
SELECT id, bal+50, bal+50, 'Thưởng thành tích học tập', now()-interval '10 days', 'seed-full' FROM s
UNION ALL
SELECT id, -50, bal, 'Đổi quà sự kiện', now()-interval '2 days', 'seed-full' FROM s;

COMMIT;

-- ---------- Tổng kết ----------
SELECT 'users' bang, count(*) n FROM users WHERE created_by='seed-full'
UNION ALL SELECT 'student_parents', count(*) FROM student_parents WHERE created_by='seed-full'
UNION ALL SELECT 'exercises', count(*) FROM exercises WHERE created_by='seed-full'
UNION ALL SELECT 'choice_options', count(*) FROM choice_options co WHERE EXISTS(SELECT 1 FROM exercises e WHERE e.id=co.exercise_id AND e.created_by='seed-full')
UNION ALL SELECT 'true_false_items', count(*) FROM true_false_items tf WHERE EXISTS(SELECT 1 FROM exercises e WHERE e.id=tf.exercise_id AND e.created_by='seed-full')
UNION ALL SELECT 'classes', count(*) FROM classes WHERE created_by='seed-full'
UNION ALL SELECT 'class_students', count(*) FROM class_students cs WHERE EXISTS(SELECT 1 FROM classes c WHERE c.id=cs.class_id AND c.created_by='seed-full')
UNION ALL SELECT 'class_sessions', count(*) FROM class_sessions WHERE created_by='seed-full'
UNION ALL SELECT 'session_attendance', count(*) FROM session_attendance WHERE created_by='seed-full'
UNION ALL SELECT 'exams', count(*) FROM exams WHERE created_by='seed-full'
UNION ALL SELECT 'exam_exercises', count(*) FROM exam_exercises ee WHERE EXISTS(SELECT 1 FROM exams e WHERE e.id=ee.exam_id AND e.created_by='seed-full')
UNION ALL SELECT 'exam_student', count(*) FROM exam_student WHERE created_by='seed-full'
UNION ALL SELECT 'exam_student DA_LAM', count(*) FROM exam_student WHERE created_by='seed-full' AND status='DA_LAM'
UNION ALL SELECT 'exam_question_result', count(*) FROM exam_question_result WHERE created_by='seed-full'
UNION ALL SELECT 'report_comments', count(*) FROM report_comments WHERE created_by='seed-full'
UNION ALL SELECT 'practice_assignments', count(*) FROM practice_assignments WHERE created_by='seed-full'
UNION ALL SELECT 'tuition_invoices', count(*) FROM tuition_invoices WHERE created_by='seed-full'
UNION ALL SELECT 'tuition_invoice_items', count(*) FROM tuition_invoice_items WHERE created_by='seed-full'
UNION ALL SELECT 'coin_wallets', count(*) FROM coin_wallets WHERE created_by='seed-full'
UNION ALL SELECT 'coin_transactions', count(*) FROM coin_transactions WHERE created_by='seed-full'
ORDER BY bang;
