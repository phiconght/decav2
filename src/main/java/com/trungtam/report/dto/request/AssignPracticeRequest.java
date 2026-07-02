package com.trungtam.report.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * examId = bai thi PH dang xem (truy vet source). topicId = chuong dang chon o
 * dropdown man chi tiet bai thi; null -> BE tu suy tu bai thi (§10.3 Buoc 0).
 */
public record AssignPracticeRequest(
        @NotNull Long examId,
        Long topicId
) {
}
