package com.trungtam.payment.entity;

import com.trungtam.identity.entity.User;
import com.trungtam.schoolclass.entity.SchoolClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mapping cua bang join {@code class_students} — CHI dung boi PricingService de doc/ghi
 * {@code discount_percent} (hoc phi rieng cua HV, SPEC_ThanhToan §0.2#4).
 * <p>Bang van co @ManyToMany tren {@link SchoolClass#getStudents()} lam owner cua cap
 * (class_id, user_id) — entity nay KHONG ghi 2 cot khoa (insertable/updatable=false) de
 * tranh tranh chap ghi. Chi cot {@code discount_percent} do entity nay quan ly.
 * Cot {@code enrolled_at} da co tu V22, chi doc.
 */
@Entity
@Table(name = "class_students")
@IdClass(ClassStudentId.class)
@Getter
@Setter
@NoArgsConstructor
public class ClassStudentPricing {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass clazz;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User student;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "enrolled_at", insertable = false, updatable = false)
    private LocalDate enrolledAt;
}
