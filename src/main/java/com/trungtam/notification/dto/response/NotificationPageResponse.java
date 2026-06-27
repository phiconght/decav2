package com.trungtam.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Response phang cho GET /notifications/me — khop ProTable FE { success, data, total }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationPageResponse(
        boolean success,
        List<NotificationItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static NotificationPageResponse of(Page<NotificationItem> page) {
        return new NotificationPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
