package com.trungtam.marketing.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.marketing.dto.response.HomeMarketingResponse;
import com.trungtam.marketing.service.MarketingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Khoi marketing Trang chu (banner khuyen mai, danh muc theo khoi lop, trust
 * bar, testimonial, hero) — dung chung cho Mobile va Trang chu cong khai
 * Web. Chi doc — {@code permitAll()} de khach chua dang nhap cung xem duoc
 * (KEHOACH_WEB_TrangChuCongKhai_HeroContent.md), khong can permission rieng.
 */
@RestController
@RequestMapping("/api/v1/home/marketing")
@RequiredArgsConstructor
public class MarketingController {

    private final MarketingService marketingService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public ApiResponse<HomeMarketingResponse> getHomeMarketing() {
        return ApiResponse.ok(marketingService.getHomeMarketing());
    }
}
