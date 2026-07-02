package com.trungtam.post.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Response phang cho danh sach bai viet — khop ProTable FE { success, data, total }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostPageResponse(
        boolean success,
        List<PostItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static PostPageResponse of(Page<PostItem> page) {
        return new PostPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
