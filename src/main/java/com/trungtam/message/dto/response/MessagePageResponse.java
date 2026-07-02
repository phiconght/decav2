package com.trungtam.message.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Response phang cho GET /messages/me — khop ProTable FE { success, data, total }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessagePageResponse(
        boolean success,
        List<MessageItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static MessagePageResponse of(Page<MessageItem> page) {
        return new MessagePageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
