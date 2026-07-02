package com.trungtam.payment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cau hinh tai khoan nhan tien (chi ADMIN) — SPEC_ThanhToan §2.9.
 */
public record UpdatePaymentSettingsRequest(
        @NotBlank @Size(max = 10) String bankBin,
        @NotBlank @Size(max = 100) String bankName,
        @NotBlank @Size(max = 30) String accountNumber,
        @NotBlank @Size(max = 100) String accountName
) {}
