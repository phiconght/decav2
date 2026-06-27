package com.trungtam.room.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/** Response phang cho GET /rooms — khop ProTable { success, data, total }. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomPageResponse(
        boolean success,
        List<RoomItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static RoomPageResponse of(Page<RoomItem> page) {
        return new RoomPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
