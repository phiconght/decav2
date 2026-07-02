package com.trungtam.payment.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Xac nhan hang loat cac dot thu (SPEC_ThanhToan §2.4). */
public record ConfirmBatchRequest(
        @NotEmpty List<Long> ids
) {}
