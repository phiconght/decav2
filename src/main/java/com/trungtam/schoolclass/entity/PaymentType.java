package com.trungtam.schoolclass.entity;

/**
 * Hinh thuc thanh toan hoc phi cua 1 LOP (ADMIN chon khi tao/sua lop — moi
 * lop chi 1 hinh thuc duy nhat, khong phai theo tung hoc vien).
 */
public enum PaymentType {
    /** Thanh toan truoc: HOC SINH tu dang ky + tra Xu tron khoa ngay (xem ClassService#enrollSelf, coinPrice). */
    PREPAID_COIN,
    /** Thanh toan sau: ADMIN chot dot thu hang thang, HS/PH chuyen khoan theo ma (xem InvoiceService). */
    POSTPAID_TRANSFER
}
