package com.trungtam.schoolclass.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.schoolclass.entity.SchoolClass;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClassDetailResponse(
        Long id,
        String code,
        String name,
        Long subjectId,
        String subjectName,
        String gradeLevel,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        BigDecimal pricePerSession,
        Long coinPrice,
        String paymentType,
        String deliveryMode,
        List<StudentOptionResponse> teachers,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClassDetailResponse from(SchoolClass c) {
        return new ClassDetailResponse(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getSubject().getId(),
                c.getSubject().getName(),
                c.getSubject().getGradeLevel(),
                c.getStartDate(),
                c.getEndDate(),
                c.getStatus().name(),
                c.getPricePerSession(),
                c.getCoinPrice(),
                c.getPaymentType().name(),
                c.getDeliveryMode().name(),
                c.getTeachers().stream().map(StudentOptionResponse::from).toList(),
                c.getCreatedBy(),
                c.getUpdatedBy(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
