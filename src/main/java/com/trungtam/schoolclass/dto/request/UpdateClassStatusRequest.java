package com.trungtam.schoolclass.dto.request;

import com.trungtam.schoolclass.entity.ClassStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateClassStatusRequest(@NotNull ClassStatus status) {}
