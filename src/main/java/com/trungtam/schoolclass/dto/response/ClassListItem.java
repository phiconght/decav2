package com.trungtam.schoolclass.dto.response;

import com.trungtam.schoolclass.entity.SchoolClass;

import java.time.Instant;
import java.time.LocalDate;

public record ClassListItem(
        Long id,
        String code,
        String name,
        Long subjectId,
        String subjectName,
        String gradeLevel,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        long studentCount,
        long examCount,
        String createdBy,
        Instant createdAt
) {
    public static ClassListItem from(SchoolClass c, long studentCount, long examCount) {
        return new ClassListItem(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getSubject().getId(),
                c.getSubject().getName(),
                c.getSubject().getGradeLevel(),
                c.getStartDate(),
                c.getEndDate(),
                c.getStatus().name(),
                studentCount,
                examCount,
                c.getCreatedBy(),
                c.getCreatedAt()
        );
    }
}
