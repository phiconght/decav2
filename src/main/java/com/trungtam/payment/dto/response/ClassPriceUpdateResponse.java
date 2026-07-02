package com.trungtam.payment.dto.response;

import java.math.BigDecimal;

/** Ket qua doi gia khoa: so buoi chua bat dau da duoc ap gia moi (SPEC_ThanhToan §2.9). */
public record ClassPriceUpdateResponse(
        BigDecimal pricePerSession,
        int updatedSessions
) {}
