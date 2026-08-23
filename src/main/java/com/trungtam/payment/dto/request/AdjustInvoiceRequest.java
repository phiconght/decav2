package com.trungtam.payment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Body cho POST /invoices/{id}/adjust — cong/tru truc tiep so tien hoc phi
 * cua 1 dot thu, doc lap voi buoc xac nhan (dung duoc bat cu luc nao, ke ca
 * sau khi da CONFIRMED/PAID, tru dot thu da CANCELLED).
 */
public record AdjustInvoiceRequest(
        @NotNull BigDecimal adjustmentAmount,
        String adjustmentNote
) {}
