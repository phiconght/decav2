package com.trungtam.report.dto.response;

/** Con cua phu huynh (cho PARENT chon con khi xem bao cao). */
public record ChildOption(
        Long studentId,
        String fullName,
        String username
) {
}
