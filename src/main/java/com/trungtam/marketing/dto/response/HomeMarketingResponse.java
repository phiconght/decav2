package com.trungtam.marketing.dto.response;

import java.util.List;

/**
 * Payload day du cho khoi marketing Trang chu mobile — GET /api/v1/home/marketing.
 */
public record HomeMarketingResponse(
        List<MarketingCategoryItem> categories,
        TrustStatsResponse trustStats,
        List<TestimonialItem> testimonials
) {
}
