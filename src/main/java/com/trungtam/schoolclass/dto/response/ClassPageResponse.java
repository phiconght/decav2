package com.trungtam.schoolclass.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassPageResponse(
        boolean success,
        List<ClassListItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static ClassPageResponse of(Page<ClassListItem> page) {
        return new ClassPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
