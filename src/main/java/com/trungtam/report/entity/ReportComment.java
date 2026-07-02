package com.trungtam.report.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.exam.entity.ExamStudent;
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
 * Nhan xet bao cao, neo theo (hoc vien, khoa hoc); tuy chon theo bai
 * (examStudent nullable). author_role snapshot vai tro luc viet.
 * visibleToStudent = false -> hoc sinh xem chinh minh khong doc duoc.
 */
@Entity
@Table(name = "report_comments")
@Getter
@Setter
@NoArgsConstructor
public class ReportComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_student_id")
    private ExamStudent examStudent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "author_role", nullable = false, length = 20)
    private String authorRole;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "visible_to_student", nullable = false)
    private boolean visibleToStudent = false;
}
