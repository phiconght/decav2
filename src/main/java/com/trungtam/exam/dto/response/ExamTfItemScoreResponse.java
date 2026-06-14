package com.trungtam.exam.dto.response;

import com.trungtam.exam.entity.ExamTfItemScore;

import java.math.BigDecimal;

public record ExamTfItemScoreResponse(
        Long tfItemId,
        String text,
        BigDecimal points
) {
    public static ExamTfItemScoreResponse from(ExamTfItemScore s) {
        return new ExamTfItemScoreResponse(
                s.getTfItem().getId(),
                s.getTfItem().getText(),
                s.getPoints()
        );
    }
}
