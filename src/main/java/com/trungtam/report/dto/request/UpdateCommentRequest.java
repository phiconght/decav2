package com.trungtam.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank @Size(max = 2000) String content,
        Boolean visibleToStudent
) {
}
