package com.trungtam.exam.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 1 ban ghi / 1 (de thi, hoc vien) — giu trang thai map giua de va hoc vien.
 */
@Entity
@Table(name = "exam_student", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"exam_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ExamStudent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private ExamStudentSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExamStudentStatus status = ExamStudentStatus.CHUA_PHAT_HANH;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "score", precision = 6, scale = 2)
    private BigDecimal score;
}
