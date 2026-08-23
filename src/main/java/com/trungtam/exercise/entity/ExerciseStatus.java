package com.trungtam.exercise.entity;

public enum ExerciseStatus {
    /** Vua nhap tu lo (Word/AI hoac nhap tay theo lo) — chua dung duoc, con sua/xoa duoc. */
    PENDING,
    ACTIVE,
    INACTIVE,
    /** Xoa mem — an khoi danh sach chon, giu lai de doi chieu. */
    DELETED
}
