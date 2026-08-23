package com.trungtam.payment.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.identity.entity.User;
import com.trungtam.schoolclass.entity.SchoolClass;
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
import java.time.LocalDate;

/**
 * Mot dot thu hoc phi cho 1 HV x 1 lop x 1 ky (SPEC_ThanhToan §1).
 * Snapshot so buoi/tien luc tao; chi tiet tung buoi o {@link TuitionInvoiceItem}.
 */
@Entity
@Table(name = "tuition_invoices")
@Getter
@Setter
@NoArgsConstructor
public class TuitionInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass clazz;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "session_count", nullable = false)
    private Integer sessionCount;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal grossAmount;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    @Column(name = "payment_code", nullable = false, unique = true, length = 40)
    private String paymentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "note", length = 500)
    private String note;

    /** So tien +/- Admin dieu chinh luc confirm (vd sai lech thuc te so voi tinh tu dong). */
    @Column(name = "adjustment_amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;

    @Column(name = "adjustment_note", length = 500)
    private String adjustmentNote;
}
