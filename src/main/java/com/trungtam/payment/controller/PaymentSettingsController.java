package com.trungtam.payment.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.payment.dto.request.UpdatePaymentSettingsRequest;
import com.trungtam.payment.dto.response.PaymentSettingsResponse;
import com.trungtam.payment.service.PaymentSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cau hinh TK nhan tien (SPEC_ThanhToan §2.9). GET = FEE:READ; PUT = chi ADMIN.
 */
@RestController
@RequestMapping("/api/v1/payment-settings")
@RequiredArgsConstructor
public class PaymentSettingsController {

    private final PaymentSettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasAuthority('FEE:READ')")
    public ApiResponse<PaymentSettingsResponse> get() {
        return ApiResponse.ok(settingsService.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentSettingsResponse> update(
            @Valid @RequestBody UpdatePaymentSettingsRequest request) {
        return ApiResponse.ok(settingsService.update(request));
    }
}
