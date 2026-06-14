package com.trungtam.exam.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TfItemScoreRequest(
        @NotNull Long tfItemId,
        @NotNull BigDecimal points
) {}
