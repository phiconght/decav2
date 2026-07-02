package com.trungtam.payment.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.schedule.entity.ClassSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Snapshot 1 buoi tinh phi trong dot thu (ngay + gia luc chot) — SPEC_ThanhToan §0.2#8.
 */
@Entity
@Table(name = "tuition_invoice_items", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tii_invoice_session", columnNames = {"invoice_id", "session_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class TuitionInvoiceItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private TuitionInvoice invoice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "price", nullable = false, precision = 12, scale = 0)
    private BigDecimal price;
}
