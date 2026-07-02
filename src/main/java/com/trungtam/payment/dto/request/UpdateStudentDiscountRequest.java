package com.trungtam.payment.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Dat giam gia % rieng cho 1 HV trong lop (0..100) — SPEC_ThanhToan §2.9. */
public record UpdateStudentDiscountRequest(
        @NotNull
        @DecimalMin(value = "0", message = "Muc giam gia phai tu 0 den 100")
        @DecimalMax(value = "100", message = "Muc giam gia phai tu 0 den 100")
        BigDecimal discountPercent
) {}
