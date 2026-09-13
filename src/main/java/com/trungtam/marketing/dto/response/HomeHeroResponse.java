package com.trungtam.marketing.dto.response;

import com.trungtam.marketing.entity.HomeHero;

/**
 * Noi dung Hero cho Trang chu cong khai Web — GET /api/v1/home/marketing
 * (khach) va GET /api/v1/admin/home/hero (quan tri).
 */
public record HomeHeroResponse(
        String badgeText,
        String title,
        String subtitle,
        String primaryCtaLabel,
        String primaryCtaHref,
        String secondaryCtaLabel,
        String secondaryCtaHref,
        String backgroundImageUrl,
        boolean visible
) {
    public static HomeHeroResponse from(HomeHero h) {
        return new HomeHeroResponse(
                h.getBadgeText(), h.getTitle(), h.getSubtitle(),
                h.getPrimaryCtaLabel(), h.getPrimaryCtaHref(),
                h.getSecondaryCtaLabel(), h.getSecondaryCtaHref(),
                h.getBackgroundImageUrl(), h.isVisible());
    }
}
