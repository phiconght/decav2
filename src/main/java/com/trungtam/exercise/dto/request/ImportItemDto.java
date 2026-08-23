package com.trungtam.exercise.dto.request;

import com.trungtam.exercise.entity.ExerciseDifficulty;
import com.trungtam.exercise.entity.ExerciseType;

import java.util.List;

/** 1 phan tu trong items[] cua file du lieu — SPEC_NhapBaiTap_TuWord_QuaAI.md §5.2. */
public record ImportItemDto(
        ExerciseType type,
        String title,
        ExerciseDifficulty difficulty,
        String questionText,
        Boolean hasImagePlaceholder,
        String imageNote,
        // MULTIPLE_CHOICE
        List<ImportOptionDto> options,
        // ESSAY
        String essayAnswer,
        // TRUE_FALSE
        List<ImportTrueFalseItemDto> trueFalseItems
) {}
