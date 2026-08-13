package com.trungtam.schedule.entity;

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

import java.time.Instant;

/**
 * Diem danh cua 1 hoc vien trong 1 buoi hoc.
 */
@Entity
@Table(name = "session_attendance", uniqueConstraints = {
        @UniqueConstraint(name = "uq_session_attendance", columnNames = {"session_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class SessionAttendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ClassSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private AttendanceStatus status = AttendanceStatus.CHUA_CHECKIN;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    /**
     * GV/Admin/nhan vien xac nhan diem danh nay dung — BAT BUOC truoc khi
     * tinh vao bao cao (yeu cau nguoi dung 13/08/2026). Bat ky lan doi
     * status nao (tu check-in/out cua HV lan setAttendance thu cong) deu
     * reset 2 truong nay ve null, buoc xac nhan lai.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;
}
