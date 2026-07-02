package com.trungtam.payment.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Tham so loc + phan trang danh sach dot thu (SPEC_ThanhToan §2.9). */
@Getter
@Setter
public class InvoiceSearchParams {

    private Long classId;
    private Long studentId;
    private String status;
    private int current = 1;
    private int pageSize = 10;
    private String sortField;
    private String sortOrder;
}
