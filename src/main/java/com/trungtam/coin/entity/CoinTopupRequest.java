package com.trungtam.coin.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Yeu cau nap Xu bang chuyen khoan (VietQR) — ty le co dinh 1.000 VND = 1 Xu.
 * HOC SINH tao yeu cau (chua cong Xu), ADMIN doi soat ngan hang roi xac
 * nhan (confirm) de cong Xu qua {@link com.trungtam.coin.service.CoinService#adjust}.
 */
@Entity
@Table(name = "coin_topup_requests")
@Getter
@Setter
@NoArgsConstructor
public class CoinTopupRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "amount_vnd", nullable = false, precision = 12, scale = 0)
    private BigDecimal amountVnd;

    @Column(name = "coin_amount", nullable = false)
    private Long coinAmount;

    @Column(name = "payment_code", nullable = false, unique = true, length = 40)
    private String paymentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private CoinTopupStatus status = CoinTopupStatus.PENDING;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "note", length = 500)
    private String note;

    /** Xu +/- Admin dieu chinh luc confirm (vd chuyen khoan thuc te lech so voi yeu cau). */
    @Column(name = "adjustment_coin_amount", nullable = false)
    private Long adjustmentCoinAmount = 0L;

    @Column(name = "adjustment_note", length = 500)
    private String adjustmentNote;
}
