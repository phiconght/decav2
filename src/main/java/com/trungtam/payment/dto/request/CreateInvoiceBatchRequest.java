package com.trungtam.payment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Tao dot thu hang loat cho 1 lop trong 1 ky (SPEC_ThanhToan §2.9).
 * studentIds = null/rong -> ca lop.
 */
public record CreateInvoiceBatchRequest(
        @NotNull Long classId,
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        List<Long> studentIds
) {}
