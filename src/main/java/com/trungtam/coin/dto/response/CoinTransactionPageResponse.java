package com.trungtam.coin.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Response phang cho lich su Xu — khop ProTable FE { success, data, total }.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CoinTransactionPageResponse(
        boolean success,
        List<CoinTransactionItem> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static CoinTransactionPageResponse of(Page<CoinTransactionItem> page) {
        return new CoinTransactionPageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
