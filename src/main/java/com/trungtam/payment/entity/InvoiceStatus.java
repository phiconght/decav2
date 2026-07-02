package com.trungtam.payment.entity;

/**
 * Trang thai dot thu hoc phi (SPEC_ThanhToan §0.2#7).
 * DRAFT -> CONFIRMED -> PAID (+ CANCELLED). Mobile chi thay CONFIRMED/PAID.
 */
public enum InvoiceStatus {
    DRAFT,
    CONFIRMED,
    PAID,
    CANCELLED
}
