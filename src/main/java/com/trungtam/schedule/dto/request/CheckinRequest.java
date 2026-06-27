package com.trungtam.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Token QR cho check-in / check-out. */
public record CheckinRequest(
        @NotBlank String token
) {
}
