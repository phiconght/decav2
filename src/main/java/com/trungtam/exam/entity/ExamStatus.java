package com.trungtam.exam.entity;

public enum ExamStatus {
    /** Tao tu lo nhap (Word/AI) — chua dung duoc, cho xac nhan cap de thi rieng. */
    PENDING,
    ACTIVE,
    INACTIVE,
    /** Xoa mem — an khoi danh sach chon, giu lai de doi chieu. */
    DELETED
}
