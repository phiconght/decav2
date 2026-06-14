package com.trungtam.exam.entity;

import com.trungtam.exercise.entity.TrueFalseItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "exam_tf_item_scores")
@Getter
@Setter
@NoArgsConstructor
public class ExamTfItemScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_exercise_id", nullable = false)
    private ExamExercise examExercise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tf_item_id", nullable = false)
    private TrueFalseItem tfItem;

    @Column(name = "points", nullable = false, precision = 6, scale = 2)
    private BigDecimal points = BigDecimal.ZERO;
}
