package com.trungtam.report.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.exam.entity.Exam;
import com.trungtam.identity.entity.User;
import com.trungtam.schoolclass.entity.SchoolClass;
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

/**
 * 1 lan PH/GV giao bai luyen tap cho 1 HV. exam = de luyen sinh ra;
 * sourceExam = bai thi PH dang xem luc bam giao (truy vet). status ASSIGNED|SUBMITTED.
 */
@Entity
@Table(name = "practice_assignments")
@Getter
@Setter
@NoArgsConstructor
public class PracticeAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false, unique = true)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_exam_id")
    private Exam sourceExam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(name = "status", nullable = false, length = 15)
    private String status = "ASSIGNED";
}
