package com.trungtam.schedule.entity;

/**
 * Trang thai buoi hoc. Chuyen PLANNED -> IN_PROGRESS -> DONE tu dong qua
 * SessionStateJob (quet moi phut, theo gio bat dau/ket thuc thuc te) —
 * khong phai client tu tinh gio.
 */
public enum SessionStatus {
    PLANNED,
    IN_PROGRESS,
    CANCELLED,
    DONE
}
