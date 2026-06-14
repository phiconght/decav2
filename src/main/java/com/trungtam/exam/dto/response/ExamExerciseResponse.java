package com.trungtam.exam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.exam.entity.ExamExercise;

import java.math.BigDecimal;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExamExerciseResponse(
        Long examExerciseId,
        Long exerciseId,
        String code,
        String title,
        String type,
        int sortOrder,
        BigDecimal points,
        List<ExamTfItemScoreResponse> itemScores
) {
    public static ExamExerciseResponse from(ExamExercise ee) {
        List<ExamTfItemScoreResponse> scores = ee.getItemScores().isEmpty()
                ? null
                : ee.getItemScores().stream().map(ExamTfItemScoreResponse::from).toList();
        return new ExamExerciseResponse(
                ee.getId(),
                ee.getExercise().getId(),
                ee.getExercise().getCode(),
                ee.getExercise().getTitle(),
                ee.getExercise().getType().name(),
                ee.getSortOrder(),
                ee.getPoints(),
                scores
        );
    }
}
