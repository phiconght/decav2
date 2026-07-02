package com.trungtam.announcement.dto.request;

import com.trungtam.announcement.entity.AudienceType;
import jakarta.validation.constraints.NotNull;

/**
 * Dem so nguoi se nhan (hien truoc khi bam Gui).
 */
public record PreviewAudienceRequest(
        @NotNull AudienceType audience,
        String audienceRef
) {
}
