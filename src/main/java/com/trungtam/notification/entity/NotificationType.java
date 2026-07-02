package com.trungtam.notification.entity;

/**
 * Loai thong bao nghiep vu (§5.2 SPEC_LichHoc_CHITIET).
 */
public enum NotificationType {
    MISSING_CHECKIN,
    MISSING_CHECKOUT,
    CHECKIN_OK,
    CHECKOUT_OK,
    LEAVE_SUBMITTED,
    LEAVE_RESULT,
    SCHEDULE_CHANGED,
    SESSION_REMINDER,
    ANNOUNCEMENT,
    // Hoc phi (SPEC_ThanhToan §2.5)
    FEE_CONFIRMED,
    FEE_PAID,
    // Giao bai luyen tap (SPEC_BaoCao §10)
    PRACTICE_ASSIGNED,
    PRACTICE_SUBMITTED
}
