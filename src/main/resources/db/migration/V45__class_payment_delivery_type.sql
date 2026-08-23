-- =====================================================================
-- V45: 2 thuoc tinh moi cho LOP (ADMIN chon, khong phai theo tung HV):
--   1) payment_type: PREPAID_COIN (tra Xu tron khoa ngay luc tu dang ky)
--      hoac POSTPAID_TRANSFER (chuyen khoan theo ma, ADMIN chot hang thang
--      qua flow tuition_invoices da co san).
--   2) delivery_mode: ONLINE (tu bam nut Diem danh) hoac OFFLINE (QR xoay
--      vong / GV diem danh — luong cu).
-- Backfill du lieu cu: lop da co coin_price > 0 -> PREPAID_COIN (dang dung
-- Xu roi); con lai -> POSTPAID_TRANSFER (dang dung invoice VND san co).
-- Moi lop cu deu la OFFLINE (dung QR/GV diem danh nhu tu truoc den nay).
-- =====================================================================

ALTER TABLE classes ADD COLUMN payment_type VARCHAR(20) NOT NULL DEFAULT 'PREPAID_COIN';
ALTER TABLE classes ADD COLUMN delivery_mode VARCHAR(20) NOT NULL DEFAULT 'OFFLINE';

UPDATE classes SET payment_type = 'POSTPAID_TRANSFER' WHERE coin_price IS NULL OR coin_price <= 0;
