package com.trungtam.schedule.entity;

import com.trungtam.common.entity.BaseEntity;
import com.trungtam.identity.entity.User;
import com.trungtam.room.entity.Room;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Mot buoi hoc cu the (materialize tu quy tac hoac tao thu cong / day bu).
 */
@Entity
@Table(name = "class_sessions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_class_session", columnNames = {"class_id", "session_date", "start_time"})
})
@Getter
@Setter
@NoArgsConstructor
public class ClassSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass clazz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ClassSchedule schedule;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private SessionStatus status = SessionStatus.PLANNED;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "is_manual", nullable = false)
    private boolean isManual = false;

    /** Gia buoi (snapshot tu classes.price_per_session luc sinh buoi) — SPEC_ThanhToan §0.2#2. */
    @Column(name = "price", nullable = false, precision = 12, scale = 0)
    private BigDecimal price = BigDecimal.ZERO;

    /** true = admin da chinh gia buoi bang tay -> doi gia khoa se bo qua buoi nay. */
    @Column(name = "price_overridden", nullable = false)
    private boolean priceOverridden = false;

    /** Gio ket thuc = gio bat dau + thoi luong. */
    public LocalTime endTime() {
        if (startTime == null || durationMinutes == null) {
            return null;
        }
        return startTime.plusMinutes(durationMinutes);
    }
}
