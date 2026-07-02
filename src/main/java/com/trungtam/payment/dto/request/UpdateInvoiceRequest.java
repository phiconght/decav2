package com.trungtam.payment.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Sua tay dot thu (CHI khi DRAFT): so tien + ghi chu — SPEC_ThanhToan §2.9. */
public record UpdateInvoiceRequest(
        @PositiveOrZero BigDecimal amount,
        @Size(max = 500) String note
) {}
