package com.trungtam.payment.repository;

import com.trungtam.payment.entity.InvoiceStatus;
import com.trungtam.payment.entity.TuitionInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TuitionInvoiceRepository
        extends JpaRepository<TuitionInvoice, Long>, JpaSpecificationExecutor<TuitionInvoice> {

    boolean existsByPaymentCode(String paymentCode);

    /**
     * HV da co dot thu (khac CANCELLED) trung ky cho lop nay chua? Neu co tra id (dau tien).
     * Dung cho preview (existingInvoiceId) va chan tao trung trong batch.
     */
    @Query("""
        SELECT i.id FROM TuitionInvoice i
        WHERE i.student.id = :studentId AND i.clazz.id = :classId
          AND i.periodFrom = :from AND i.periodTo = :to
          AND i.status <> com.trungtam.payment.entity.InvoiceStatus.CANCELLED
        ORDER BY i.id ASC
        """)
    List<Long> findExistingInvoiceIds(@Param("studentId") Long studentId,
                                      @Param("classId") Long classId,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    default Optional<Long> findExistingInvoiceId(Long studentId, Long classId,
                                                 LocalDate from, LocalDate to) {
        List<Long> ids = findExistingInvoiceIds(studentId, classId, from, to);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    List<TuitionInvoice> findByStudentIdAndStatusInOrderByIdDesc(Long studentId,
                                                                 List<InvoiceStatus> statuses);
}
