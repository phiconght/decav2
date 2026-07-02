package com.trungtam.payment.dto.response;

import com.trungtam.payment.entity.PaymentSettings;

/** TK nhan tien (SPEC_ThanhToan §2.9). */
public record PaymentSettingsResponse(
        String bankBin,
        String bankName,
        String accountNumber,
        String accountName
) {
    public static PaymentSettingsResponse from(PaymentSettings s) {
        return new PaymentSettingsResponse(
                s.getBankBin(), s.getBankName(), s.getAccountNumber(), s.getAccountName());
    }
}
