package com.trungtam.schedule.repository;

import com.trungtam.schedule.entity.SessionTeacherAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SessionTeacherAttendanceRepository extends JpaRepository<SessionTeacherAttendance, Long> {

    Optional<SessionTeacherAttendance> findBySessionId(Long sessionId);

    boolean existsBySessionId(Long sessionId);

    /** Batch cho timetable view TEACHER: cong cua nhieu buoi cung luc. */
    List<SessionTeacherAttendance> findBySessionIdIn(Collection<Long> sessionIds);

    /** Report: cac dong cong cua 1 GV trong khoang ngay (join session). */
    @Query("""
            SELECT a FROM SessionTeacherAttendance a
            WHERE a.teacher.id = :teacherId
              AND a.session.sessionDate BETWEEN :from AND :to
            """)
    List<SessionTeacherAttendance> findByTeacherAndRange(@Param("teacherId") Long teacherId,
                                                         @Param("from") LocalDate from,
                                                         @Param("to") LocalDate to);
}
