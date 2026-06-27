package com.trungtam.leave.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Response phang cho GET /leaves — khop ProTable FE { success, data, total }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeavePageResponse(
        boolean success,
        List<LeaveItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static LeavePageResponse of(Page<LeaveItem> page) {
        return new LeavePageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
