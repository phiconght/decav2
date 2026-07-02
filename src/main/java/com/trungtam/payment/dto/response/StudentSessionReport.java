package com.trungtam.payment.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Bao cao chuyen can HV: summary + items (SPEC_ThanhToan §2.8).
 */
public record StudentSessionReport(
        Summary summary,
        List<Item> items
) {
    /** Tong hop so buoi theo trang thai + tong tien cac buoi tinh phi. */
    public record Summary(
            int total,        // tong buoi DONE
            int coMat,
            int tre,
            int vang,
            int coPhep,
            int chuaCheckin,
            BigDecimal totalAmount
    ) {}

    /** 1 buoi. */
    public record Item(
            LocalDate date,
            String className,
            LocalTime startTime,
            LocalTime endTime,
            String status,     // AttendanceStatus name
            BigDecimal price
    ) {}
}
