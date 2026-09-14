package com.trungtam.settings.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateAppSettingsRequest(@NotBlank String supportHotline) {}
