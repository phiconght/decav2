package com.trungtam.payment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Chinh gia le 1 buoi hoc (SPEC_ThanhToan §2.9). */
public record UpdateSessionPriceRequest(
        @NotNull @PositiveOrZero BigDecimal price
) {}
