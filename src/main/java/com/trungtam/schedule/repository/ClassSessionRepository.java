package com.trungtam.schedule.repository;

import com.trungtam.schedule.entity.ClassSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ClassSessionRepository
        extends JpaRepository<ClassSession, Long>, JpaSpecificationExecutor<ClassSession> {

    boolean existsByClazzIdAndSessionDateAndStartTime(Long classId, LocalDate sessionDate, LocalTime startTime);

    boolean existsByIdAndClazzId(Long id, Long classId);

    /**
     * Buoi hoc cua 1 lop trong khoang ngay.
     * EntityGraph nap san clazz/room/teacher/topic vi {@code SessionDetail.from}
     * doc het cac quan he nay — khong co graph thi moi dong sinh them query (N+1).
     */
    @EntityGraph(attributePaths = {"clazz", "room", "teacher", "topic"})
    List<ClassSession> findByClazzIdAndSessionDateBetween(Long classId, LocalDate from, LocalDate to);

    /**
     * TOAN BO buoi hoc cua 1 lop (khong loc ngay) — dung cho che do "Toan khoa"
     * o man gan chuyen de ben Admin (SPEC_KhoaHoc_NoiDung_Mobile.md §4.2) va cho
     * API outline. KHONG dung findByClazzIdAndSessionDateBetween voi from/to =
     * null: `BETWEEN null AND null` tra ve rong chu khong phai "tat ca".
     */
    @EntityGraph(attributePaths = {"clazz", "room", "teacher", "topic"})
    List<ClassSession> findByClazzId(Long classId);

    // ===== HOC PHI (SPEC_ThanhToan) =====

    /**
     * Cap nhat gia buoi cho cac buoi CHUA BAT DAU (PLANNED, session_date+start_time > now)
     * VA chua bi chinh tay (price_overridden = false) cua 1 lop. Tra so buoi da doi.
     * Native de dung phep cong time cua Postgres (make_interval theo phut).
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE class_sessions
        SET price = :price
        WHERE class_id = :classId
          AND status = 'PLANNED'
          AND price_overridden = FALSE
          AND (session_date + start_time) > CAST(:now AS timestamp)
        """, nativeQuery = true)
    int bulkUpdateFuturePrice(@Param("classId") Long classId,
                              @Param("price") BigDecimal price,
                              @Param("now") java.time.LocalDateTime now);

    /**
     * Buoi tinh phi cua 1 HV trong ky (SPEC_ThanhToan §2.4): buoi DONE trong khoang ngay,
     * TRU buoi HV nghi CO_PHEP. Tra ve theo thu tu ngay tang dan.
     */
    @Query(value = """
        SELECT s.* FROM class_sessions s
        WHERE s.class_id = :classId AND s.status = 'DONE'
          AND s.session_date BETWEEN :from AND :to
          AND NOT EXISTS (SELECT 1 FROM session_attendance sa
                          WHERE sa.session_id = s.id AND sa.user_id = :studentId
                            AND sa.status = 'CO_PHEP')
        ORDER BY s.session_date, s.start_time
        """, nativeQuery = true)
    List<ClassSession> findFeeSessions(@Param("classId") Long classId,
                                       @Param("studentId") Long studentId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    List<ClassSession> findBySessionDateAndStatus(LocalDate sessionDate,
                                                  com.trungtam.schedule.entity.SessionStatus status);

    /** Buoi sinh ra tu quy tac, tuong lai, PLANNED, khong phai thu cong (de regenerate khi sua quy tac). */
    @Query("""
        SELECT s FROM ClassSession s
        WHERE s.schedule.id = :scheduleId
          AND s.sessionDate >= :from
          AND s.status = com.trungtam.schedule.entity.SessionStatus.PLANNED
          AND s.isManual = false
        """)
    List<ClassSession> findFutureGeneratedBySchedule(@Param("scheduleId") Long scheduleId,
                                                     @Param("from") LocalDate from);

    /** Buoi PLANNED da qua gio ket thuc tinh den thoi diem mistart (cho CloseSessionJob). */
    @Query(value = """
        SELECT * FROM class_sessions s
        WHERE s.status = 'PLANNED'
          AND (s.session_date < :today
               OR (s.session_date = :today
                   AND (s.start_time + make_interval(mins => s.duration_minutes)) <= CAST(:nowTime AS time)))
        """, nativeQuery = true)
    List<ClassSession> findPlannedPastEnd(@Param("today") LocalDate today,
                                          @Param("nowTime") LocalTime nowTime);

    // ----- TRUNG PHONG: buoi cung phong, cung ngay, giao gio (NATIVE + OVERLAPS) -----
    @Query(value = """
        SELECT * FROM class_sessions s
        WHERE s.room_id = :roomId AND s.session_date = :date AND s.status <> 'CANCELLED'
          AND (:excludeId IS NULL OR s.id <> :excludeId)
          AND (s.start_time, s.start_time + make_interval(mins => s.duration_minutes))
              OVERLAPS
              (CAST(:startTime AS time), CAST(:startTime AS time) + make_interval(mins => :durationMinutes))
        """, nativeQuery = true)
    List<ClassSession> findRoomConflicts(@Param("roomId") Long roomId,
                                         @Param("date") LocalDate date,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("durationMinutes") int durationMinutes,
                                         @Param("excludeId") Long excludeId);

    // ----- TRUNG GIAO VIEN: y het nhung s.teacher_id = :teacherId -----
    @Query(value = """
        SELECT * FROM class_sessions s
        WHERE s.teacher_id = :teacherId AND s.session_date = :date AND s.status <> 'CANCELLED'
          AND (:excludeId IS NULL OR s.id <> :excludeId)
          AND (s.start_time, s.start_time + make_interval(mins => s.duration_minutes))
              OVERLAPS
              (CAST(:startTime AS time), CAST(:startTime AS time) + make_interval(mins => :durationMinutes))
        """, nativeQuery = true)
    List<ClassSession> findTeacherConflicts(@Param("teacherId") Long teacherId,
                                            @Param("date") LocalDate date,
                                            @Param("startTime") LocalTime startTime,
                                            @Param("durationMinutes") int durationMinutes,
                                            @Param("excludeId") Long excludeId);

    // ----- TRUNG HOC VIEN (canh bao): HV cua lop nay co buoi lop KHAC cung ngay & giao gio -----
    // Tra ten lop khac (chi can canh bao, khong chan).
    @Query(value = """
        SELECT DISTINCT c.name FROM class_sessions s
        JOIN class_students cs ON cs.class_id = s.class_id
        JOIN classes c ON c.id = s.class_id
        WHERE s.session_date = :date AND s.status <> 'CANCELLED'
          AND s.class_id <> :classId
          AND cs.user_id IN (:studentIds)
          AND (s.start_time, s.start_time + make_interval(mins => s.duration_minutes))
              OVERLAPS
              (CAST(:startTime AS time), CAST(:startTime AS time) + make_interval(mins => :durationMinutes))
        """, nativeQuery = true)
    List<String> findStudentConflictClassNames(@Param("classId") Long classId,
                                               @Param("studentIds") List<Long> studentIds,
                                               @Param("date") LocalDate date,
                                               @Param("startTime") LocalTime startTime,
                                               @Param("durationMinutes") int durationMinutes);

    // ----- TIMETABLE: HV — buoi cua cac lop HV ghi danh, loc tu enrolled_at -----
    @Query(value = """
        SELECT s.* FROM class_sessions s
        JOIN class_students cs ON cs.class_id = s.class_id
        WHERE cs.user_id = :userId
          AND s.session_date BETWEEN :from AND :to
          AND (cs.enrolled_at IS NULL OR s.session_date >= cs.enrolled_at)
        ORDER BY s.session_date, s.start_time
        """, nativeQuery = true)
    List<ClassSession> findTimetableForStudent(@Param("userId") Long userId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    // ----- TIMETABLE: GV — buoi gan truc tiep cho GV, HOAC lop GV phu trach khi session.teacher null -----
    @Query("""
        SELECT s FROM ClassSession s
        WHERE s.sessionDate BETWEEN :from AND :to
          AND (s.teacher.id = :userId
               OR (s.teacher IS NULL
                   AND EXISTS (SELECT 1 FROM SchoolClass c JOIN c.teachers t
                               WHERE c.id = s.clazz.id AND t.id = :userId)))
        ORDER BY s.sessionDate, s.startTime
        """)
    List<ClassSession> findTimetableForTeacher(@Param("userId") Long userId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    // ----- TIMETABLE: PHONG -----
    @Query("""
        SELECT s FROM ClassSession s
        WHERE s.room.id = :roomId AND s.sessionDate BETWEEN :from AND :to
        ORDER BY s.sessionDate, s.startTime
        """)
    List<ClassSession> findTimetableForRoom(@Param("roomId") Long roomId,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to);
}
