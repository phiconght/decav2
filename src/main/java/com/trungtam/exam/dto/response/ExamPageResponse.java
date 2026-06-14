package com.trungtam.exam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExamPageResponse(
        boolean success,
        List<ExamListItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static ExamPageResponse of(Page<ExamListItem> page) {
        return new ExamPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
