package com.trungtam.coin.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/** Response phang cho GET /coin-topups (Admin) — khop ProTable FE { success, data, total }. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CoinTopupPageResponse(
        boolean success,
        List<CoinTopupResponse> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static CoinTopupPageResponse of(Page<CoinTopupResponse> page) {
        return new CoinTopupPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
