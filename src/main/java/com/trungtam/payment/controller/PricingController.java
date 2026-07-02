package com.trungtam.payment.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.payment.dto.request.UpdateClassPriceRequest;
import com.trungtam.payment.dto.request.UpdateSessionPriceRequest;
import com.trungtam.payment.dto.request.UpdateStudentDiscountRequest;
import com.trungtam.payment.dto.response.ClassPriceUpdateResponse;
import com.trungtam.payment.dto.response.SessionPriceItem;
import com.trungtam.payment.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Gia khoa/buoi + giam gia HV (SPEC_ThanhToan §2.9).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @PutMapping("/classes/{id}/price")
    @PreAuthorize("hasAuthority('FEE:WRITE')")
    public ApiResponse<ClassPriceUpdateResponse> updateClassPrice(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassPriceRequest request) {
        return ApiResponse.ok(pricingService.updateClassPrice(id, request.pricePerSession()));
    }

    @GetMapping("/classes/{id}/sessions/prices")
    @PreAuthorize("hasAuthority('FEE:READ')")
    public ApiResponse<List<SessionPriceItem>> listSessionPrices(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(pricingService.listSessionPrices(id, from, to));
    }

    @PutMapping("/sessions/{id}/price")
    @PreAuthorize("hasAuthority('FEE:WRITE')")
    public ApiResponse<SessionPriceItem> updateSessionPrice(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSessionPriceRequest request) {
        return ApiResponse.ok(pricingService.updateSessionPrice(id, request.price()));
    }

    @PutMapping("/classes/{classId}/students/{studentId}/discount")
    @PreAuthorize("hasAuthority('FEE:WRITE')")
    public ApiResponse<Void> updateStudentDiscount(
            @PathVariable Long classId,
            @PathVariable Long studentId,
            @Valid @RequestBody UpdateStudentDiscountRequest request) {
        pricingService.updateStudentDiscount(classId, studentId, request.discountPercent());
        return ApiResponse.ok();
    }
}
