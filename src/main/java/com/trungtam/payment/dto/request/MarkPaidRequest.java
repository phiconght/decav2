package com.trungtam.payment.dto.request;

import jakarta.validation.constraints.Size;

/** Danh dau da thu (kem ghi chu doi chieu tuy chon) — SPEC_ThanhToan §2.9. */
public record MarkPaidRequest(
        @Size(max = 500) String note
) {}
