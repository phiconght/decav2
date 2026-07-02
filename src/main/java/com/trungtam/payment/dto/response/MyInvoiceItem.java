package com.trungtam.payment.dto.response;

import com.trungtam.payment.entity.InvoiceStatus;
import com.trungtam.payment.entity.TuitionInvoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Dot thu cho mobile (PH/HV) — SPEC_ThanhToan §2.1. */
public record MyInvoiceItem(
        Long id,
        Long studentId,
        Long classId,
        String className,
        LocalDate periodFrom,
        LocalDate periodTo,
        int sessionCount,
        BigDecimal amount,
        InvoiceStatus status,
        Instant paidAt
) {
    public static MyInvoiceItem from(TuitionInvoice i) {
        return new MyInvoiceItem(
                i.getId(),
                i.getStudent() != null ? i.getStudent().getId() : null,
                i.getClazz() != null ? i.getClazz().getId() : null,
                i.getClazz() != null ? i.getClazz().getName() : null,
                i.getPeriodFrom(),
                i.getPeriodTo(),
                i.getSessionCount() != null ? i.getSessionCount() : 0,
                i.getAmount(),
                i.getStatus(),
                i.getPaidAt());
    }
}
