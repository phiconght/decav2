package com.trungtam.payment.dto.response;

import com.trungtam.payment.entity.InvoiceStatus;
import com.trungtam.payment.entity.TuitionInvoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Chi tiet day du 1 dot thu + items[] (SPEC_ThanhToan §2.9). */
public record InvoiceResponse(
        Long id,
        Long studentId,
        String studentName,
        String username,
        Long classId,
        String className,
        LocalDate periodFrom,
        LocalDate periodTo,
        int sessionCount,
        BigDecimal grossAmount,
        BigDecimal discountPercent,
        BigDecimal amount,
        String paymentCode,
        InvoiceStatus status,
        Instant confirmedAt,
        Instant paidAt,
        String note,
        Instant createdAt,
        List<InvoiceItemLine> items
) {
    public static InvoiceResponse from(TuitionInvoice i, List<InvoiceItemLine> items) {
        return new InvoiceResponse(
                i.getId(),
                i.getStudent() != null ? i.getStudent().getId() : null,
                i.getStudent() != null ? i.getStudent().getFullName() : null,
                i.getStudent() != null ? i.getStudent().getUsername() : null,
                i.getClazz() != null ? i.getClazz().getId() : null,
                i.getClazz() != null ? i.getClazz().getName() : null,
                i.getPeriodFrom(),
                i.getPeriodTo(),
                i.getSessionCount() != null ? i.getSessionCount() : 0,
                i.getGrossAmount(),
                i.getDiscountPercent(),
                i.getAmount(),
                i.getPaymentCode(),
                i.getStatus(),
                i.getConfirmedAt(),
                i.getPaidAt(),
                i.getNote(),
                i.getCreatedAt(),
                items);
    }
}
