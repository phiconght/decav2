package com.trungtam.coin.dto.response;

import com.trungtam.coin.entity.CoinTransaction;

import java.time.Instant;

/**
 * 1 dong lich su Xu cho FE/mobile: so Xu +/-, so du sau, ly do, nguoi thao tac, thoi diem.
 */
public record CoinTransactionItem(
        Long id,
        Long amount,
        Long balanceAfter,
        String reason,
        String createdBy,
        Instant createdAt
) {
    public static CoinTransactionItem from(CoinTransaction t) {
        return new CoinTransactionItem(
                t.getId(),
                t.getAmount(),
                t.getBalanceAfter(),
                t.getReason(),
                t.getCreatedBy(),
                t.getCreatedAt());
    }
}
