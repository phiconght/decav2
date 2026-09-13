package com.trungtam.marketing.dto.response;

import java.util.List;

/**
 * Payload day du cho khoi marketing Trang chu — GET /api/v1/home/marketing.
 * Dung chung cho Mobile (bo qua field la khong biet) va Trang chu cong khai
 * Web ({@code hero} chi Web dung — xem
 * KEHOACH_WEB_TrangChuCongKhai_HeroContent.md muc 4.3). {@code hero} co the
 * null neu chua cau hinh hoac dang tat (visible = false).
 */
public record HomeMarketingResponse(
        HomeHeroResponse hero,
        List<MarketingCategoryItem> categories,
        TrustStatsResponse trustStats,
        List<TestimonialItem> testimonials
) {
}
