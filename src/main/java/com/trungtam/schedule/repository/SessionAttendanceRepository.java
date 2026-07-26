package com.trungtam.schedule.repository;

import com.trungtam.schedule.entity.SessionAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SessionAttendanceRepository extends JpaRepository<SessionAttendance, Long> {

    List<SessionAttendance> findBySessionId(Long sessionId);

    Optional<SessionAttendance> findBySessionIdAndUserId(Long sessionId, Long userId);

    boolean existsBySessionIdAndUserId(Long sessionId, Long userId);

    /** Diem danh cua 1 hoc vien trong khoang ngay (cho view timetable HV/PH). */
    @Query("""
        SELECT a FROM SessionAttendance a
        WHERE a.user.id = :userId
          AND a.session.sessionDate BETWEEN :from AND :to
        """)
    List<SessionAttendance> findByUserIdAndDateRange(@Param("userId") Long userId,
                                                     @Param("from") LocalDate from,
                                                     @Param("to") LocalDate to);

    /**
     * Diem danh cua 1 hoc vien tren MOI buoi cua 1 khoa — dung cho
     * {@code GET /classes/{id}/outline} de dinh trang thai diem danh vao tung
     * dong buoi hoc bang 1 query duy nhat (thay vi hoi tung buoi).
     */
    @Query("""
        SELECT a FROM SessionAttendance a
        WHERE a.user.id = :userId
          AND a.session.clazz.id = :classId
        """)
    List<SessionAttendance> findByUserIdAndClassId(@Param("userId") Long userId,
                                                   @Param("classId") Long classId);
}
