package com.trungtam.coin.controller;

import com.trungtam.coin.dto.request.AdjustCoinTopupRequest;
import com.trungtam.coin.dto.request.CoinTopupSearchParams;
import com.trungtam.coin.dto.request.CreateCoinTopupRequest;
import com.trungtam.coin.dto.response.CoinTopupPageResponse;
import com.trungtam.coin.dto.response.CoinTopupResponse;
import com.trungtam.coin.service.CoinTopupService;
import com.trungtam.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Nap Xu bang chuyen khoan (VietQR, ty le 1.000 VND = 1 Xu, Admin xac nhan thu cong). */
@RestController
@RequestMapping("/api/v1/coin-topups")
@RequiredArgsConstructor
public class CoinTopupController {

    private final CoinTopupService topupService;

    // ---- Mobile/Web (self/con) ----

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CoinTopupResponse> create(@Valid @RequestBody CreateCoinTopupRequest request) {
        return ApiResponse.ok(topupService.create(request));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<CoinTopupResponse>> myTopups(
            @RequestParam(required = false) Long studentId) {
        return ApiResponse.ok(topupService.myTopups(studentId));
    }

    // ---- Admin / Employee (COIN:*) ----

    @GetMapping
    @PreAuthorize("hasAuthority('COIN:READ')")
    public CoinTopupPageResponse list(@ModelAttribute CoinTopupSearchParams params) {
        return topupService.list(params);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('COIN:WRITE')")
    public ApiResponse<CoinTopupResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok(topupService.confirm(id));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('COIN:WRITE')")
    public ApiResponse<CoinTopupResponse> adjust(
            @PathVariable Long id,
            @Valid @RequestBody AdjustCoinTopupRequest request) {
        return ApiResponse.ok(
                topupService.adjust(id, request.adjustmentCoinAmount(), request.adjustmentNote()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('COIN:WRITE')")
    public ApiResponse<CoinTopupResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(topupService.cancel(id));
    }
}
