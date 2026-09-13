package com.trungtam.marketing.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.marketing.dto.request.HomeHeroUpdateRequest;
import com.trungtam.marketing.dto.response.HomeHeroResponse;
import com.trungtam.marketing.service.MarketingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quan tri noi dung Hero cua Trang chu cong khai Web (ADMIN / EMPLOYEE) —
 * gop vao muc "Noi Dung" cua app ADMIN, dung lai quyen MARKETING:*
 * (V41__home_marketing.sql — da cap san cho ADMIN + EMPLOYEE, chua tung
 * duoc controller nao dung). Xem
 * KEHOACH_WEB_TrangChuCongKhai_HeroContent.md muc 4.8.
 */
@RestController
@RequestMapping("/api/v1/admin/home")
@RequiredArgsConstructor
public class AdminHomeContentController {

    private final MarketingService marketingService;

    @GetMapping("/hero")
    @PreAuthorize("hasAuthority('MARKETING:READ')")
    public ApiResponse<HomeHeroResponse> getHero() {
        return ApiResponse.ok(marketingService.getHeroForAdmin());
    }

    @PutMapping("/hero")
    @PreAuthorize("hasAuthority('MARKETING:WRITE')")
    public ApiResponse<HomeHeroResponse> updateHero(@Valid @RequestBody HomeHeroUpdateRequest req) {
        return ApiResponse.ok(marketingService.updateHero(req));
    }
}
