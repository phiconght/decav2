package com.trungtam.schoolclass.entity;

/** Hinh thuc hoc cua 1 LOP — quyet dinh cach diem danh. */
public enum DeliveryMode {
    /** Hoc truc tuyen — HS tu bam nut "Điểm danh" khi vao buoi hoc (xem ScheduleService#checkinOnlineSelf). */
    ONLINE,
    /** Hoc truc tiep tai lop — diem danh qua QR xoay vong hoac GV/Admin diem danh thu cong (luong cu). */
    OFFLINE
}
