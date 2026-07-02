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
    SESSION_REMINDER
}
