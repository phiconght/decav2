package com.trungtam.schoolclass.dto.response;

/** Ket qua HS tu dang ky tham gia 1 lop bang Xu — GV goi POST /classes/{id}/enroll. */
public record EnrollResponse(
        Long classId,
        String className,
        long coinSpent,
        long newBalance
) {}
