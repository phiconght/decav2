package com.trungtam.subject.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubjectPageResponse(
        boolean success,
        List<SubjectListItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static SubjectPageResponse of(Page<SubjectListItem> page) {
        return new SubjectPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
