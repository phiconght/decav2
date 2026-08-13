package com.trungtam.leave.dto.response;

import com.trungtam.leave.entity.LeaveRequest;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Dong hien thi don nghi cho danh sach / chi tiet.
 */
public record LeaveItem(
        Long id,
        Long studentId,
        String studentName,
        String scope,
        Long sessionId,
        LocalDate sessionDate,
        Long classId,
        String className,
        LocalDate dateFrom,
        LocalDate dateTo,
        String reason,
        String status,
        String reviewedBy,
        Instant reviewedAt,
        String parentConfirmedBy,
        Instant parentConfirmedAt,
        Instant createdAt
) {
    public static LeaveItem from(LeaveRequest e) {
        return new LeaveItem(
                e.getId(),
                e.getStudent() != null ? e.getStudent().getId() : null,
                e.getStudent() != null ? e.getStudent().getFullName() : null,
                e.getScope() != null ? e.getScope().name() : null,
                e.getSession() != null ? e.getSession().getId() : null,
                e.getSession() != null ? e.getSession().getSessionDate() : null,
                e.getClazz() != null ? e.getClazz().getId() : null,
                e.getClazz() != null ? e.getClazz().getName() : null,
                e.getDateFrom(),
                e.getDateTo(),
                e.getReason(),
                e.getStatus() != null ? e.getStatus().name() : null,
                e.getReviewedBy() != null ? e.getReviewedBy().getFullName() : null,
                e.getReviewedAt(),
                e.getParentConfirmedBy() != null ? e.getParentConfirmedBy().getFullName() : null,
                e.getParentConfirmedAt(),
                e.getCreatedAt());
    }
}
