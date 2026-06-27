package com.trungtam.room.repository;

import com.trungtam.room.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    List<Holiday> findByHolidayDateBetweenOrderByHolidayDateAsc(LocalDate from, LocalDate to);

    boolean existsByHolidayDateAndBranchId(LocalDate holidayDate, Long branchId);

    /** Trung ngay nghi toan he thong (branch_id IS NULL) — derived query khong loc duoc NULL bang '='. */
    boolean existsByHolidayDateAndBranchIsNull(LocalDate holidayDate);

    /** Ngay nghi ap dung cho 1 co so: rieng co so do HOAC toan he thong (branch null). */
    @Query("""
        SELECT h.holidayDate FROM Holiday h
        WHERE h.holidayDate BETWEEN :from AND :to
          AND (h.branch IS NULL OR h.branch.id = :branchId)
        """)
    List<LocalDate> findDatesInRange(@Param("from") LocalDate from,
                                     @Param("to") LocalDate to,
                                     @Param("branchId") Long branchId);

    /** Ngay nghi toan he thong (khi buoi khong gan phong -> khong biet co so). */
    @Query("SELECT h.holidayDate FROM Holiday h WHERE h.holidayDate BETWEEN :from AND :to AND h.branch IS NULL")
    List<LocalDate> findGlobalDatesInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
