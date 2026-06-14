package com.trungtam.schoolclass.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddStudentsRequest(@NotEmpty List<Long> studentIds) {}
