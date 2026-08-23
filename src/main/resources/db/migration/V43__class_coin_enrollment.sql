-- =====================================================================
-- V43: Hoc sinh dung Xu tu dang ky tham gia khoa hoc.
-- Them gia Xu RIENG cho tung lop (khac pricePerSession — von la don gia
-- tra GV, khong phai gia ban cho HV). NULL/0 = lop khong mo ban qua Xu.
-- =====================================================================

ALTER TABLE classes ADD COLUMN coin_price BIGINT;

COMMENT ON COLUMN classes.coin_price IS
    'Gia Xu de hoc sinh tu dang ky tham gia lop qua Mobile/Web. NULL/0 = khong mo ban qua Xu (chi admin them thu cong).';
