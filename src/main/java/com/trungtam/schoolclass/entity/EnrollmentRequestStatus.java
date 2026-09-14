package com.trungtam.schoolclass.entity;

/** Trang thai yeu cau dang ky khoa hoc (truoc khi ghi danh) — xem {@link ClassEnrollmentRequest}. */
public enum EnrollmentRequestStatus {
    /** Da tao QR, dang cho Admin doi chieu chuyen khoan. */
    PENDING,
    /** Admin da xac nhan nhan duoc tien va ghi danh hoc vien. */
    CONFIRMED,
    /** Admin huy (khong thay chuyen khoan, sai thong tin...). */
    CANCELLED
}
