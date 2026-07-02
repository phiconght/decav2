package com.trungtam.payment.dto.response;

import com.trungtam.payment.entity.TuitionInvoiceItem;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 1 buoi trong chi tiet dot thu (SPEC_ThanhToan §2.9 — InvoiceResponse.items[]). */
public record InvoiceItemLine(
        Long sessionId,
        LocalDate sessionDate,
        BigDecimal price
) {
    public static InvoiceItemLine from(TuitionInvoiceItem it) {
        return new InvoiceItemLine(
                it.getSession() != null ? it.getSession().getId() : null,
                it.getSessionDate(),
                it.getPrice());
    }
}
