package com.trungtam.payment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trungtam.common.dto.ApiError;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/** Response phang cho GET /invoices — khop ProTable FE { success, data, total }. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InvoicePageResponse(
        boolean success,
        List<InvoiceResponse> data,
        long total,
        ApiError error,
        Instant timestamp
) {
    public static InvoicePageResponse of(Page<InvoiceResponse> page) {
        return new InvoicePageResponse(
                true, page.getContent(), page.getTotalElements(), null, Instant.now());
    }
}
