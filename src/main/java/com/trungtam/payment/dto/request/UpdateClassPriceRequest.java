package com.trungtam.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Doi don gia/buoi cua khoa hoc (SPEC_ThanhToan §2.9). */
public record UpdateClassPriceRequest(
        @NotNull @PositiveOrZero BigDecimal pricePerSession
) {}
