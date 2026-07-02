package com.trungtam.payment.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.payment.dto.request.UpdatePaymentSettingsRequest;
import com.trungtam.payment.dto.response.PaymentSettingsResponse;
import com.trungtam.payment.entity.PaymentSettings;
import com.trungtam.payment.repository.PaymentSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cau hinh tai khoan nhan tien (single-row, id=1) — SPEC_ThanhToan §2.9.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentSettingsService {

    private final PaymentSettingsRepository repository;

    /** Chua cau hinh -> PAYMENT_SETTINGS_MISSING. */
    public PaymentSettingsResponse get() {
        PaymentSettings s = repository.findById(PaymentSettings.SINGLETON_ID)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_SETTINGS_MISSING));
        return PaymentSettingsResponse.from(s);
    }

    /** Upsert (chi ADMIN — gate o controller). */
    @Transactional
    public PaymentSettingsResponse update(UpdatePaymentSettingsRequest req) {
        PaymentSettings s = repository.findById(PaymentSettings.SINGLETON_ID)
                .orElseGet(() -> {
                    PaymentSettings ns = new PaymentSettings();
                    ns.setId(PaymentSettings.SINGLETON_ID);
                    return ns;
                });
        s.setBankBin(req.bankBin());
        s.setBankName(req.bankName());
        s.setAccountNumber(req.accountNumber());
        s.setAccountName(req.accountName());
        return PaymentSettingsResponse.from(repository.save(s));
    }
}
