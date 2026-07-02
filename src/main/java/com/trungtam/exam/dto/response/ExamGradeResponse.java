package com.trungtam.exam.dto.response;

import java.util.List;

/**
 * Ket qua cham bai: MC/TF tu cham, tu luan cho cham tay (correct = null).
 */
public record ExamGradeResponse(
        double earned,
        double total,
        int autoCorrect,
        int autoTotal,
        boolean hasEssay,
        List<QuestionGrade> byQuestion
) {
    public record QuestionGrade(
            Long examExerciseId,
            double earned,
            double max,
            Boolean correct
    ) {
    }
}
