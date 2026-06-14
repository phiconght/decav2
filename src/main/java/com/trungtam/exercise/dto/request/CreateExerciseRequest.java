package com.trungtam.exercise.dto.request;

import com.trungtam.exercise.entity.ExerciseStatus;
import com.trungtam.exercise.entity.ExerciseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateExerciseRequest(
        String title,
        @NotNull Long subjectId,
        @NotNull ExerciseType type,
        ExerciseStatus status,
        @NotBlank String questionText,
        String questionImage,
        // ESSAY
        String essayAnswer,
        String essayAnswerImage,
        // MULTIPLE_CHOICE
        @Valid List<ChoiceOptionRequest> options,
        // TRUE_FALSE
        @Valid List<TrueFalseItemRequest> trueFalseItems
) {}
