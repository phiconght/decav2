package com.trungtam.payment.repository;

import com.trungtam.payment.entity.PaymentSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentSettingsRepository extends JpaRepository<PaymentSettings, Long> {
}
