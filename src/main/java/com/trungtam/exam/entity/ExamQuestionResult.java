package com.trungtam.exam.entity;

import com.trungtam.common.entity.BaseEntity;
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

/**
 * Ket qua cham TUNG CAU cua 1 bai nop (snapshot luc submit / backfill).
 * Nen tang cho bao cao: breakdown theo do kho / loai cau / chuyen de.
 * correct = null nghia la cau tu luan (chua cham tay -> "cho cham").
 */
@Entity
@Table(name = "exam_question_result", uniqueConstraints = {
        @UniqueConstraint(name = "uq_eqr_student_exercise",
                columnNames = {"exam_student_id", "exam_exercise_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class ExamQuestionResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_student_id", nullable = false)
    private ExamStudent examStudent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_exercise_id", nullable = false)
    private ExamExercise examExercise;

    @Column(name = "earned", nullable = false, precision = 6, scale = 2)
    private BigDecimal earned = BigDecimal.ZERO;

    @Column(name = "max_points", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxPoints = BigDecimal.ZERO;

    /** TRUE/FALSE cho cau cham tu dong; NULL = tu luan (cho cham tay). */
    @Column(name = "correct")
    private Boolean correct;
}
