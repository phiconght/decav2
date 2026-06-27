-- =====================================================================
-- V17: Seed Chuyên đề + gán Chuyên đề cho Bài tập / Đề thi (tự chạy khi restart)
-- FK logic: topic.subject_id = subject; exercise/exam.topic_id thuộc ĐÚNG môn.
-- Idempotent để an toàn dù dữ liệu đã có sẵn.
-- =====================================================================

-- 1) CHUYÊN ĐỀ THEO MÔN HỌC (gắn cho mọi khối 6..12 của từng môn)
INSERT INTO topics (subject_id, name, sort_order)
SELECT s.id, v.topic_name, v.sort_order
FROM subjects s
JOIN (VALUES
    ('Toán',       'Đại số',               1),
    ('Toán',       'Hình học',             2),
    ('Toán',       'Số học',               3),
    ('Toán',       'Thống kê & Xác suất',  4),

    ('Vật lý',     'Cơ học',               1),
    ('Vật lý',     'Nhiệt học',            2),
    ('Vật lý',     'Điện & Từ',            3),
    ('Vật lý',     'Quang học',            4),

    ('Hóa học',    'Hóa đại cương',        1),
    ('Hóa học',    'Hóa vô cơ',            2),
    ('Hóa học',    'Hóa hữu cơ',           3),

    ('Ngữ văn',    'Đọc hiểu',             1),
    ('Ngữ văn',    'Tiếng Việt',           2),
    ('Ngữ văn',    'Làm văn',              3),

    ('Tiếng Anh',  'Ngữ pháp',             1),
    ('Tiếng Anh',  'Từ vựng',              2),
    ('Tiếng Anh',  'Đọc hiểu',             3),
    ('Tiếng Anh',  'Nghe - Nói',           4),

    ('Sinh học',   'Tế bào học',           1),
    ('Sinh học',   'Di truyền học',        2),
    ('Sinh học',   'Sinh thái học',        3),

    ('Tin học',    'Lập trình',            1),
    ('Tin học',    'Thuật toán',           2),
    ('Tin học',    'Cơ sở dữ liệu',        3)
) AS v(subject_name, topic_name, sort_order)
  ON s.name = v.subject_name
WHERE NOT EXISTS (
    SELECT 1 FROM topics t
    WHERE t.subject_id = s.id AND t.name = v.topic_name
);

-- 2) GÁN CHUYÊN ĐỀ CHO BÀI TẬP (chỉ bài chưa có; chuyên đề cùng môn; phân bổ theo id)
UPDATE exercises e
SET topic_id = pick.topic_id
FROM (
    SELECT e2.id AS exercise_id, ranked.topic_id
    FROM exercises e2
    JOIN LATERAL (
        SELECT t.id AS topic_id,
               ROW_NUMBER() OVER (ORDER BY t.sort_order, t.id) AS rn,
               COUNT(*)     OVER ()                            AS cnt
        FROM topics t
        WHERE t.subject_id = e2.subject_id
    ) ranked ON ranked.rn = (e2.id % ranked.cnt) + 1
    WHERE e2.topic_id IS NULL
) pick
WHERE e.id = pick.exercise_id;

-- 3) GÁN CHUYÊN ĐỀ CHO ĐỀ THI (chỉ đề chưa có; chuyên đề cùng môn)
UPDATE exams ex
SET topic_id = pick.topic_id
FROM (
    SELECT ex2.id AS exam_id, ranked.topic_id
    FROM exams ex2
    JOIN LATERAL (
        SELECT t.id AS topic_id,
               ROW_NUMBER() OVER (ORDER BY t.sort_order, t.id) AS rn,
               COUNT(*)     OVER ()                            AS cnt
        FROM topics t
        WHERE t.subject_id = ex2.subject_id
    ) ranked ON ranked.rn = (ex2.id % ranked.cnt) + 1
    WHERE ex2.topic_id IS NULL
) pick
WHERE ex.id = pick.exam_id;
