package com.trungtam.exam.dto.request;

import com.trungtam.exam.entity.ExamStudentStatus;
import jakarta.validation.constraints.NotNull;

/** Admin doi trang thai de cho 1 hoc vien (DA_PHAT_HANH / CHUA_PHAT_HANH / DA_XOA). */
public record UpdateExamStudentStatusRequest(@NotNull ExamStudentStatus status) {}
