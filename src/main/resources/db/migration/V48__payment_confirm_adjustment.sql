-- Cho phep Admin cong/tru mot khoan dieu chinh ngay luc xac nhan thanh toan
-- (nap Xu / hoa don hoc phi) de phan anh dung so tien thuc te cua hoc vien.

ALTER TABLE coin_topup_requests
    ADD COLUMN adjustment_coin_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN adjustment_note VARCHAR(500);

ALTER TABLE tuition_invoices
    ADD COLUMN adjustment_amount NUMERIC(12, 0) NOT NULL DEFAULT 0,
    ADD COLUMN adjustment_note VARCHAR(500);
