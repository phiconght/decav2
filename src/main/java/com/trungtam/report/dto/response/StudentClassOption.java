package com.trungtam.report.dto.response;

/** Khoa hoc cua 1 HV (cho dropdown chon khoa khi xem bao cao). */
public record StudentClassOption(
        Long classId,
        String code,
        String name,
        String subjectName,
        String teacherNames
) {
}
