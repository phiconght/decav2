package com.trungtam.exercise.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Xac nhan hang loat cac bai tap PENDING -> ACTIVE. */
public record ConfirmExercisesRequest(@NotEmpty List<Long> ids) {}
