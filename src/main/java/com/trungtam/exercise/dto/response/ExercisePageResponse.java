package com.trungtam.exercise.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Response phang cho GET /exercises — khop voi ProTable FE can { success, data, total }.
 * Dung record rieng thay vi ApiResponse<T> vi can truong "total" ngang cap voi "data".
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExercisePageResponse(
        boolean success,
        List<ExerciseListItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static ExercisePageResponse of(Page<ExerciseListItem> page) {
        return new ExercisePageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
