package com.trungtam.payment.entity;

import com.trungtam.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cau hinh tai khoan nhan tien (VietQR). Chi 1 dong duy nhat (id = 1) — SPEC_ThanhToan §1.
 * Id gan tay (khong @GeneratedValue) vi bang chi co 1 hang.
 */
@Entity
@Table(name = "payment_settings")
@Getter
@Setter
@NoArgsConstructor
public class PaymentSettings extends BaseEntity {

    /** Luon = 1 (single-row). */
    public static final long SINGLETON_ID = 1L;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "bank_bin", nullable = false, length = 10)
    private String bankBin;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;
}
