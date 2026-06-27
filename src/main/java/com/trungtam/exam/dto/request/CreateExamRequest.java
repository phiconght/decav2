package com.trungtam.exam.dto.request;

import com.trungtam.exam.entity.ExamStatus;
import com.trungtam.exam.entity.ExamType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.List;

public record CreateExamRequest(
        @NotBlank String name,
        @NotNull Long subjectId,
        Long topicId,
        @NotNull ExamType type,
        @Positive Integer durationMinutes,
        Instant publishAt,
        Instant endAt,
        ExamStatus status,
        @Valid List<ExamExerciseRequest> exercises,
        List<Long> classIds,
        List<Long> studentIds
) {}
