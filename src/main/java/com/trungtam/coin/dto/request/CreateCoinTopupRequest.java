package com.trungtam.coin.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** HOC SINH tao yeu cau nap Xu bang chuyen khoan. Ty le co dinh 1.000 VND = 1 Xu. */
public record CreateCoinTopupRequest(
        @NotNull @Positive BigDecimal amountVnd
) {}
