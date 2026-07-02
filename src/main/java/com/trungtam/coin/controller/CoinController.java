package com.trungtam.coin.controller;

import com.trungtam.coin.dto.request.AdjustCoinRequest;
import com.trungtam.coin.dto.response.CoinBalanceResponse;
import com.trungtam.coin.dto.response.CoinTransactionPageResponse;
import com.trungtam.coin.service.CoinService;
import com.trungtam.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API Xu hoc vien (tien ao):
 * - Admin/Employee tra cuu so du + lich su; chi Admin (COIN:WRITE) cong/tru.
 * - Mobile (HV/PH) xem so du + lich su cua minh/con qua /coins/my (ownership trong service).
 */
@RestController
@RequestMapping("/api/v1/coins")
@RequiredArgsConstructor
public class CoinController {

    private final CoinService coinService;

    // ---- Mobile (self/con) — khai bao truoc de "/my" khong dinh vao "/{studentId}" ----

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<CoinBalanceResponse> myBalance(
            @RequestParam(required = false) Long studentId) {
        return ApiResponse.ok(coinService.myBalance(studentId));
    }

    @GetMapping("/my/transactions")
    @PreAuthorize("isAuthenticated()")
    public CoinTransactionPageResponse myTransactions(
            @RequestParam(required = false) Long studentId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        return coinService.myHistory(studentId, current, pageSize);
    }

    // ---- Admin / Employee ----

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAuthority('COIN:READ')")
    public ApiResponse<CoinBalanceResponse> balance(@PathVariable Long studentId) {
        return ApiResponse.ok(coinService.balance(studentId));
    }

    @GetMapping("/{studentId}/transactions")
    @PreAuthorize("hasAuthority('COIN:READ')")
    public CoinTransactionPageResponse transactions(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        return coinService.history(studentId, current, pageSize);
    }

    @PostMapping("/{studentId}/adjust")
    @PreAuthorize("hasAuthority('COIN:WRITE')")
    public ApiResponse<CoinBalanceResponse> adjust(
            @PathVariable Long studentId,
            @Valid @RequestBody AdjustCoinRequest request) {
        return ApiResponse.ok(coinService.adjust(studentId, request.amount(), request.reason()));
    }
}
