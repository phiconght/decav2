package com.trungtam.payment.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.payment.dto.request.AdjustInvoiceRequest;
import com.trungtam.payment.dto.request.ConfirmBatchRequest;
import com.trungtam.payment.dto.request.CreateInvoiceBatchRequest;
import com.trungtam.payment.dto.request.InvoiceSearchParams;
import com.trungtam.payment.dto.request.MarkPaidRequest;
import com.trungtam.payment.dto.request.UpdateInvoiceRequest;
import com.trungtam.payment.dto.response.InvoicePageResponse;
import com.trungtam.payment.dto.response.InvoicePreviewItem;
import com.trungtam.payment.dto.response.InvoiceQrResponse;
import com.trungtam.payment.dto.response.InvoiceResponse;
import com.trungtam.payment.dto.response.MyInvoiceItem;
import com.trungtam.payment.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Dot thu hoc phi (SPEC_ThanhToan §2.9). Endpoint my/qr = isAuthenticated + ownership trong service.
 */
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping("/preview")
    @PreAuthorize("hasAuthority('FEE:READ')")
    public ApiResponse<List<InvoicePreviewItem>> preview(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(invoiceService.preview(classId, from, to));
    }

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEE:WRITE')")
    public ApiResponse<List<InvoiceResponse>> createBatch(
            @Valid @RequestBody CreateInvoiceBatchRequest request) {
        return ApiResponse.ok(invoiceService.createBatch(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FEE:READ')")
    public InvoicePageResponse list(@ModelAttribute InvoiceSearchParams params) {
        return invoiceService.list(params);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FEE:READ')")
    public ApiResponse<InvoiceResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(invoiceService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FEE:WRITE')")
    public ApiResponse<InvoiceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateInvoiceRequest request) {
        return ApiResponse.ok(invoiceService.update(id, request));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('FEE:WRITE')")
    public ApiResponse<InvoiceResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok(invoiceService.confirm(id));
    }

    @PostMapping("/{id}/adjust")
    @PreAuthorize("hasAuthority('FEE:WRITE')")
    public ApiResponse<InvoiceResponse> adjust(
            @PathVariable Long id,
            @Valid @RequestBody AdjustInvoiceRequest request) {
        return ApiResponse.ok(
                invoiceService.adjust(id, request.adjustmentAmount(), request.adjustmentNote()));
    }

    @PostMapping("/confirm-batch")
    @PreAuthorize("hasAuthority('FEE:WRITE')")
    public ApiResponse<List<InvoiceResponse>> confirmBatch(
            @Valid @RequestBody ConfirmBatchRequest request) {
        return ApiResponse.ok(invoiceService.confirmBatch(request.ids()));
    }

    @PostMapping("/{id}/paid")
    @PreAuthorize("hasAuthority('FEE:WRITE')")
    public ApiResponse<InvoiceResponse> markPaid(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) MarkPaidRequest request) {
        String note = request != null ? request.note() : null;
        return ApiResponse.ok(invoiceService.markPaid(id, note));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('FEE:WRITE')")
    public ApiResponse<InvoiceResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(invoiceService.cancel(id));
    }

    // ---- Mobile (self/con) ----

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MyInvoiceItem>> myInvoices(
            @RequestParam(required = false) Long studentId) {
        return ApiResponse.ok(invoiceService.myInvoices(studentId));
    }

    @GetMapping("/{id}/qr")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<InvoiceQrResponse> qr(@PathVariable Long id) {
        return ApiResponse.ok(invoiceService.qr(id));
    }
}
