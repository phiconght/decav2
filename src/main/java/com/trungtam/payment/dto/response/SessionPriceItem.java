package com.trungtam.payment.dto.response;

import com.trungtam.schedule.entity.ClassSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** 1 buoi trong bang gia (SPEC_ThanhToan §2.9 — GET /classes/{id}/sessions/prices). */
public record SessionPriceItem(
        Long sessionId,
        LocalDate date,
        LocalTime time,
        String status,
        BigDecimal price,
        boolean priceOverridden
) {
    public static SessionPriceItem from(ClassSession s) {
        return new SessionPriceItem(
                s.getId(),
                s.getSessionDate(),
                s.getStartTime(),
                s.getStatus() != null ? s.getStatus().name() : null,
                s.getPrice(),
                s.isPriceOverridden());
    }
}
