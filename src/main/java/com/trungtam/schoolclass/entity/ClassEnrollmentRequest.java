package com.trungtam.schoolclass.entity;

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
 * 1 yeu cau dang ky khoa hoc (truoc khi ghi danh) — hoc vien bam "Dang ky" o
 * Card/trang chi tiet, nhan QR chuyen khoan; Admin doi chieu sao ke thu cong
 * roi bam "Xac nhan &amp; Ghi danh" (xem docs kem theo thiet ke).
 */
@Entity
@Table(name = "class_enrollment_requests")
@Getter
@Setter
@NoArgsConstructor
public class ClassEnrollmentRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** Snapshot classes.full_price luc bam Dang ky — khoa doi gia sau khong anh huong yeu cau dang cho. */
    @Column(name = "amount", nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;

    /** Noi dung chuyen khoan de Admin doi chieu — dang "{ma khoa}-{4 ky tu}". */
    @Column(name = "registration_code", nullable = false, unique = true, length = 40)
    private String registrationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private EnrollmentRequestStatus status = EnrollmentRequestStatus.PENDING;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;
}
