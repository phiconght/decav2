package com.trungtam.video.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/** Response phang cho GET /lecture-videos — khop ProTable { success, data, total }. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LectureVideoPageResponse(
        boolean success,
        List<LectureVideoItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static LectureVideoPageResponse of(Page<LectureVideoItem> page) {
        return new LectureVideoPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
