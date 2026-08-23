package com.trungtam.exercise.entity;

/** Trang thai 1 lo nhap bai tap/de thi tu file du lieu (Word/AI hoac nhap tay theo lo). */
public enum ImportBatchStatus {
    /** Con it nhat 1 cau PENDING chua xu ly (xac nhan hoac xoa) trong lo. */
    IN_PROGRESS,
    /** Toan bo cau trong lo da ra khoi PENDING (ACTIVE hoac DELETED het). */
    COMPLETED,
    /** Du tru cho thao tac huy ca lo (chua co UI o v1). */
    ABANDONED
}
