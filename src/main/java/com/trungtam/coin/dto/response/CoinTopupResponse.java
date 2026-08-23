package com.trungtam.coin.dto.response;

import com.trungtam.coin.entity.CoinTopupRequest;
import com.trungtam.coin.entity.CoinTopupStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Chi tiet 1 yeu cau nap Xu, kem payload QR de render ngay (khong can goi rieng). */
public record CoinTopupResponse(
        Long id,
        Long studentId,
        String studentName,
        String username,
        BigDecimal amountVnd,
        Long coinAmount,
        String paymentCode,
        CoinTopupStatus status,
        Instant confirmedAt,
        String note,
        Instant createdAt,
        String qrPayload,
        String bankName,
        String accountNumber,
        String accountName,
        Long adjustmentCoinAmount,
        String adjustmentNote
) {
    public static CoinTopupResponse from(CoinTopupRequest r, String qrPayload,
                                         String bankName, String accountNumber, String accountName) {
        return new CoinTopupResponse(
                r.getId(),
                r.getStudent() != null ? r.getStudent().getId() : null,
                r.getStudent() != null ? r.getStudent().getFullName() : null,
                r.getStudent() != null ? r.getStudent().getUsername() : null,
                r.getAmountVnd(),
                r.getCoinAmount(),
                r.getPaymentCode(),
                r.getStatus(),
                r.getConfirmedAt(),
                r.getNote(),
                r.getCreatedAt(),
                qrPayload,
                bankName,
                accountNumber,
                accountName,
                r.getAdjustmentCoinAmount(),
                r.getAdjustmentNote());
    }
}
