package com.trungtam.exercise.dto.request;

import com.trungtam.exercise.entity.ExerciseStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull ExerciseStatus status) {}
