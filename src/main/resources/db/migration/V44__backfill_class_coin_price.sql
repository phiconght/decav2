-- =====================================================================
-- V44: Dien gia Xu cho cac lop THAT dang ACTIVE (truoc gio coin_price
-- toan NULL tru vai dong test thu cong) — de "Khám phá khóa học" va khoi
-- marketing Trang chu co gia hien thi day du thay vi trong.
--
-- Cong thuc: 150.000 + (khoi - 6) * 20.000 + (id % 5) * 10.000 — tang dan
-- theo khoi lop (khoi cao hoc phi cao hon), co bien thien nho giua cac
-- lop cung khoi de khong bi dong gia mot cach gia tao. Admin van co the
-- sua tay tung lop qua form (KE_HOACH_TRIEN_KHAI.md) — day chi la du lieu
-- khoi tao hop ly, khong phai gia tri cuoi cung.
-- Lop INACTIVE giu coin_price NULL (dung — khong ban khoa da ket thuc).
-- =====================================================================

UPDATE classes c
SET coin_price = 150000
    + (CAST(regexp_replace(s.grade_level, '[^0-9]', '', 'g') AS INTEGER) - 6) * 20000
    + (c.id % 5) * 10000
FROM subjects s
WHERE c.subject_id = s.id
  AND c.status = 'ACTIVE'
  AND c.coin_price IS NULL;
