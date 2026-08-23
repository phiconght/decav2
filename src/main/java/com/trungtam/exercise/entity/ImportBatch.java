package com.trungtam.exercise.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.exam.entity.Exam;
import com.trungtam.subject.entity.Subject;
import com.trungtam.topic.entity.Topic;
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

/**
 * Metadata 1 lan nhap bai tap/de thi theo lo (Word/AI -> file du lieu, hoac
 * nhap tay theo lo) — xem SPEC_NhapBaiTap_TuWord_QuaAI.md §2. Khong luu lai
 * noi dung JSON goc, chi la header hien thi tren man duyet lo; noi dung that
 * nam o cac Exercise (Exercise.importBatch) da tao.
 */
@Entity
@Table(name = "import_batches")
@Getter
@Setter
@NoArgsConstructor
public class ImportBatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_file_name", nullable = false)
    private String sourceFileName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "exam_name")
    private String examName;

    /** Chi co gia tri neu lo nay tao kem 1 de thi ("Import Đề Thi"). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImportBatchStatus status = ImportBatchStatus.IN_PROGRESS;
}
