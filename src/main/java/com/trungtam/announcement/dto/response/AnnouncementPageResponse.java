package com.trungtam.announcement.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Response phang cho lich su thong bao — khop ProTable FE { success, data, total }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnnouncementPageResponse(
        boolean success,
        List<AnnouncementItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static AnnouncementPageResponse of(Page<AnnouncementItem> page) {
        return new AnnouncementPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
