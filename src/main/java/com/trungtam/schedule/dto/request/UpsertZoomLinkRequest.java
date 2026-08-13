package com.trungtam.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Tao / cap nhat 1 link Zoom cua buoi hoc. {@code label} rong -> "Link chinh". */
public record UpsertZoomLinkRequest(
        String label,
        @NotBlank String zoomUrl,
        String meetingId,
        String passcode
) {}
