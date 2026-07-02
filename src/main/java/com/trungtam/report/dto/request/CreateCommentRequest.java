package com.trungtam.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotNull Long studentId,
        @NotNull Long classId,
        Long examStudentId,
        @NotBlank @Size(max = 2000) String content,
        boolean visibleToStudent
) {
}
