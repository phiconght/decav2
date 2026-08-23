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
 * Khoi marketing Trang chu mobile (banner khuyen mai, danh muc theo khoi
 * lop, trust bar, testimonial). Chi doc — moi vai tro da dang nhap deu
 * xem duoc (khong can permission rieng, giong pattern PostController.feed()).
 */
@RestController
@RequestMapping("/api/v1/home/marketing")
@RequiredArgsConstructor
public class MarketingController {

    private final MarketingService marketingService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<HomeMarketingResponse> getHomeMarketing() {
        return ApiResponse.ok(marketingService.getHomeMarketing());
    }
}
