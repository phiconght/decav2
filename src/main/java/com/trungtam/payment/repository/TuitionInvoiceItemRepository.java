package com.trungtam.payment.repository;

import com.trungtam.payment.entity.TuitionInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TuitionInvoiceItemRepository extends JpaRepository<TuitionInvoiceItem, Long> {

    List<TuitionInvoiceItem> findByInvoiceIdOrderBySessionDateAsc(Long invoiceId);
}
