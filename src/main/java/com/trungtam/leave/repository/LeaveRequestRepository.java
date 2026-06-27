package com.trungtam.leave.repository;

import com.trungtam.leave.entity.LeaveRequest;
import com.trungtam.leave.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    /**
     * Hoc vien co don nghi da DUYET ap dung cho 1 buoi cu the khong?
     * - SESSION: don tro thang den session do.
     * - RANGE: session_date nam trong [date_from, date_to] va (clazz null = tat ca lop
     *   hoac clazz trung lop cua buoi).
     */
    @Query("""
        SELECT CASE WHEN COUNT(l) > 0 THEN TRUE ELSE FALSE END
        FROM LeaveRequest l
        WHERE l.student.id = :studentId
          AND l.status = :approved
          AND (
                (l.scope = com.trungtam.leave.entity.LeaveScope.SESSION
                    AND l.session.id = :sessionId)
             OR (l.scope = com.trungtam.leave.entity.LeaveScope.RANGE
                    AND :sessionDate BETWEEN l.dateFrom AND l.dateTo
                    AND (l.clazz IS NULL OR l.clazz.id = :classId))
          )
        """)
    boolean isOnLeave(@Param("studentId") Long studentId,
                      @Param("sessionId") Long sessionId,
                      @Param("sessionDate") LocalDate sessionDate,
                      @Param("classId") Long classId,
                      @Param("approved") LeaveStatus approved);
}
