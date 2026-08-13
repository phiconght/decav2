package com.trungtam.exam.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Xac nhan hang loat — danh sach id exam_student (khong phai examId). */
public record ConfirmExamsRequest(
        @NotEmpty List<Long> examStudentIds
) {
}
