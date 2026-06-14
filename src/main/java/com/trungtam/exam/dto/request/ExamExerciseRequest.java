package com.trungtam.exam.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ExamExerciseRequest(
        @NotNull Long exerciseId,
        Integer sortOrder,
        BigDecimal points,
        @Valid List<TfItemScoreRequest> itemScores
) {}
