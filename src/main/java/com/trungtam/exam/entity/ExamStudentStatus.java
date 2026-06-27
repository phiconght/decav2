package com.trungtam.exam.entity;

/**
 * Trang thai map giua De thi <-> Hoc vien (luu thang 1 cot).
 * QUA_HAN khong luu o day: duoc tinh luc doc khi now > exam.endAt va chua nop.
 */
public enum ExamStudentStatus {
    CHUA_PHAT_HANH,
    DA_PHAT_HANH,
    DANG_KIEM_TRA,
    DA_LAM,
    DA_XOA
}
