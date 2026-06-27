package com.trungtam.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Huy 1 buoi hoc (kem ly do). */
public record CancelSessionRequest(
        @NotBlank String reason
) {
}
