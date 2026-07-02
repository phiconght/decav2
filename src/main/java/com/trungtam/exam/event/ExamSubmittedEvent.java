package com.trungtam.exam.event;

/**
 * Phat khi hoc vien nop bai (sau khi persist ket qua). Module report nghe de
 * xu ly de luyen tap (§10) — tach vong phu thuoc: exam khong phu thuoc report.
 */
public record ExamSubmittedEvent(Long examStudentId) {
}
