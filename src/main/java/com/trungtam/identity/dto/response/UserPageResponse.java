package com.trungtam.identity.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Response phang cho GET /admin/users — khop voi ProTable FE can { success, data, total }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserPageResponse(
        boolean success,
        List<UserListItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static UserPageResponse of(Page<UserListItem> page) {
        return new UserPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
