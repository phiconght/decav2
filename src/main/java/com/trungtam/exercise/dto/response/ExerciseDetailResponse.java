package com.trungtam.exercise.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.exercise.entity.Exercise;
import com.trungtam.exercise.entity.ExerciseType;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExerciseDetailResponse(
        Long id,
        String code,
        String title,
        Long subjectId,
        String subjectName,
        String gradeLevel,
        Long topicId,
        String topicName,
        String type,
        String difficulty,
        String status,
        String questionText,
        String questionImage,
        // ESSAY
        String essayAnswer,
        String essayAnswerImage,
        // MULTIPLE_CHOICE
        List<ChoiceOptionResponse> options,
        // TRUE_FALSE
        List<TrueFalseItemResponse> trueFalseItems,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExerciseDetailResponse from(Exercise e) {
        List<ChoiceOptionResponse> opts = null;
        List<TrueFalseItemResponse> tfItems = null;
        String essayAns = null;
        String essayAnsImg = null;

        if (e.getType() == ExerciseType.MULTIPLE_CHOICE) {
            opts = e.getOptions().stream().map(ChoiceOptionResponse::from).toList();
        } else if (e.getType() == ExerciseType.ESSAY) {
            essayAns = e.getEssayAnswer();
            essayAnsImg = e.getEssayAnswerImage();
        } else if (e.getType() == ExerciseType.TRUE_FALSE) {
            tfItems = e.getTrueFalseItems().stream().map(TrueFalseItemResponse::from).toList();
        }

        return new ExerciseDetailResponse(
                e.getId(),
                e.getCode(),
                e.getTitle(),
                e.getSubjectEntity().getId(),
                e.getSubjectEntity().getName(),
                e.getSubjectEntity().getGradeLevel(),
                e.getTopic() != null ? e.getTopic().getId() : null,
                e.getTopic() != null ? e.getTopic().getName() : null,
                e.getType().name(),
                e.getDifficulty().name(),
                e.getStatus().name(),
                e.getQuestionText(),
                e.getQuestionImage(),
                essayAns,
                essayAnsImg,
                opts,
                tfItems,
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
