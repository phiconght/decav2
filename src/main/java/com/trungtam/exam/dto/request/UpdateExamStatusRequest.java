package com.trungtam.exam.dto.request;

import com.trungtam.exam.entity.ExamStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateExamStatusRequest(@NotNull ExamStatus status) {}
