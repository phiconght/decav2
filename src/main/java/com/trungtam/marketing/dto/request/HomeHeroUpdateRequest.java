package com.trungtam.marketing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Yeu cau sua noi dung Hero Trang chu — PUT /api/v1/admin/home/hero.
 */
public record HomeHeroUpdateRequest(
        @Size(max = 60) String badgeText,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 400) String subtitle,
        @Size(max = 60) String primaryCtaLabel,
        @Size(max = 255) String primaryCtaHref,
        @Size(max = 60) String secondaryCtaLabel,
        @Size(max = 255) String secondaryCtaHref,
        @Size(max = 500) String backgroundImageUrl,
        boolean visible
) {
}
