package com.trungtam.exam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/** Response phang cho GET /exam-confirmations — khop ProTable FE { success, data, total }. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExamConfirmationPageResponse(
        boolean success,
        List<ExamConfirmationItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static ExamConfirmationPageResponse of(Page<ExamConfirmationItem> page) {
        return new ExamConfirmationPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
