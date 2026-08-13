package com.trungtam.leave.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.identity.entity.User;
import com.trungtam.schedule.entity.ClassSession;
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

import java.time.Instant;
import java.time.LocalDate;

/**
 * Don xin nghi cua hoc vien. SESSION = nghi 1 buoi; RANGE = nghi theo khoang ngay
 * (theo 1 lop neu clazz != null, hoac tat ca lop neu clazz == null).
 */
@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 10)
    private LeaveScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ClassSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass clazz;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private LeaveStatus status = LeaveStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    /**
     * PHU HUYNH xac nhan don nghi cua con — BAT BUOC truoc khi GV/nhan vien
     * (khong phai ADMIN) duoc duyet (yeu cau nguoi dung 13/08/2026). Neu
     * chinh phu huynh la nguoi tao don (requestedBy la PH) thi tu dong coi
     * nhu da xac nhan ngay luc tao. ADMIN duyet duoc bat ky luc nao, bo qua
     * dieu kien nay.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_confirmed_by")
    private User parentConfirmedBy;

    @Column(name = "parent_confirmed_at")
    private Instant parentConfirmedAt;
}
