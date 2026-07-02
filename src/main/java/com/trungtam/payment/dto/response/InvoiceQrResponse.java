package com.trungtam.payment.dto.response;

import java.math.BigDecimal;

/** Payload VietQR + thong tin TK cho mobile render QR (SPEC_ThanhToan §2.9). */
public record InvoiceQrResponse(
        String qrPayload,
        String bankName,
        String accountNumber,
        String accountName,
        BigDecimal amount,
        String paymentCode
) {}
