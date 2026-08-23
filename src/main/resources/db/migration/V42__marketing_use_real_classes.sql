-- =====================================================================
-- V42: Khoi marketing Trang chu chuyen sang dung LOP HOC THAT (bang
-- classes/subjects) thay vi bang promoted_courses seed rieng — dam bao
-- danh muc "Luyện thi vào 10 / Khối THCS / Khối THPT" LUON DONG BO voi
-- danh sach that trong "Khám phá khóa học" (yeu cau nguoi dung).
-- =====================================================================

ALTER TABLE marketing_categories ADD COLUMN grade_filter VARCHAR(50);

UPDATE marketing_categories SET grade_filter = '9'          WHERE banner_tag = 'KHỐI 9';
UPDATE marketing_categories SET grade_filter = '6,7,8,9'    WHERE banner_tag = 'THCS';
UPDATE marketing_categories SET grade_filter = '10,11,12'   WHERE banner_tag = 'THPT';

DROP TABLE IF EXISTS promoted_courses;
