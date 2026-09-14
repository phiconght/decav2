package com.trungtam.schoolclass.dto.response;

import com.trungtam.schoolclass.entity.ClassEnrollmentRequest;

import java.math.BigDecimal;

/**
 * Ket qua bam "Dang ky" / xem lai yeu cau dang ky cua chinh minh — kem QR
 * chuyen khoan (VietQR, tai dung {@code VietQrService} cua module Hoc phi).
 */
public record RegistrationResponse(
        Long id,
        Long classId,
        String className,
        BigDecimal amount,
        String registrationCode,
        String status,
        String qrPayload,
        String bankName,
        String accountNumber,
        String accountName
) {
    public static RegistrationResponse from(
            ClassEnrollmentRequest r, String qrPayload, String bankName, String accountNumber, String accountName) {
        return new RegistrationResponse(
                r.getId(),
                r.getSchoolClass().getId(),
                r.getSchoolClass().getName(),
                r.getAmount(),
                r.getRegistrationCode(),
                r.getStatus().name(),
                qrPayload,
                bankName,
                accountNumber,
                accountName
        );
    }
}
