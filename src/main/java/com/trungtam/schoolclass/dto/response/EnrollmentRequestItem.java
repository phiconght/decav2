package com.trungtam.schoolclass.dto.response;

import com.trungtam.schoolclass.entity.ClassEnrollmentRequest;

import java.math.BigDecimal;
import java.time.Instant;

/** 1 dong trong man Admin "Yêu cầu đăng ký khóa học". */
public record EnrollmentRequestItem(
        Long id,
        Long classId,
        String className,
        Long studentId,
        String studentUsername,
        String studentFullName,
        BigDecimal amount,
        String registrationCode,
        String status,
        Instant createdAt,
        Instant confirmedAt
) {
    public static EnrollmentRequestItem from(ClassEnrollmentRequest r) {
        return new EnrollmentRequestItem(
                r.getId(),
                r.getSchoolClass().getId(),
                r.getSchoolClass().getName(),
                r.getStudent().getId(),
                r.getStudent().getUsername(),
                r.getStudent().getFullName() != null ? r.getStudent().getFullName() : r.getStudent().getUsername(),
                r.getAmount(),
                r.getRegistrationCode(),
                r.getStatus().name(),
                r.getCreatedAt(),
                r.getConfirmedAt()
        );
    }
}
