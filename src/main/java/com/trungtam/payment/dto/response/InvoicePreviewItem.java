package com.trungtam.payment.dto.response;

import java.math.BigDecimal;

/**
 * 1 dong preview dot thu cho 1 HV (SPEC_ThanhToan §2.4). KHONG ghi DB.
 * existingInvoiceId != null -> HV da co dot thu trung ky (FE disable checkbox).
 */
public record InvoicePreviewItem(
        Long studentId,
        String fullName,
        String username,
        int sessionCount,
        BigDecimal grossAmount,
        BigDecimal discountPercent,
        BigDecimal amount,
        Long existingInvoiceId
) {}
