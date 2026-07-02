package com.trungtam.exam.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.trungtam.exam.dto.request.SubmitExamRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * De thi day du cho hoc vien lam bai (self-scoped).
 *
 * Dap an dung (isCorrect / answer / essayAnswer*) CHI tra ve khi bai o trang
 * thai chi-xem (DA_LAM / QUA_HAN); khi dang lam luon la null de tranh lo dap an.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExamPaperResponse(
        Long examId,
        String code,
        String name,
        Integer durationMinutes,
        Instant deadline,
        String status,
        List<PaperQuestion> questions,
        SubmitExamRequest submitted,
        BigDecimal score,
        ExamGradeResponse result
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaperQuestion(
            Long examExerciseId,
            Long exerciseId,
            String type,
            double points,
            String questionText,
            String questionImage,
            List<PaperOption> options,
            List<PaperTfItem> trueFalseItems,
            String essayAnswer,
            String essayAnswerImage
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaperOption(
            Long id,
            int order,
            String text,
            String image,
            @JsonProperty("isCorrect") Boolean isCorrect
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaperTfItem(
            Long id,
            int order,
            String text,
            String image,
            Boolean answer
    ) {
    }
}
