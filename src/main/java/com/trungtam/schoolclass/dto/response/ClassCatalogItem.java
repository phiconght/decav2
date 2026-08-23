package com.trungtam.schoolclass.dto.response;

import com.trungtam.schoolclass.entity.SchoolClass;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 1 dong trong danh muc TOAN HE THONG (moi lop, ke ca lop chua ghi danh) —
 * nguon DUY NHAT cho man "Khám phá khóa học" (Mobile) va khoi marketing
 * Trang chu (nhom theo khoi lop). CHI DOC, khong lo dung de mo chi tiet
 * buoi hoc/de thi (yeu cau nguoi dung 11/08/2026).
 */
public record ClassCatalogItem(
        Long id,
        String code,
        String name,
        String subjectName,
        String gradeLevel,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal pricePerSession,
        Long coinPrice,
        String paymentType,
        String deliveryMode,
        boolean enrolled,
        List<String> teacherNames
) {
    public static ClassCatalogItem from(SchoolClass c, boolean enrolled) {
        return new ClassCatalogItem(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getSubject().getName(),
                c.getSubject().getGradeLevel(),
                c.getStatus().name(),
                c.getStartDate(),
                c.getEndDate(),
                c.getPricePerSession(),
                c.getCoinPrice(),
                c.getPaymentType().name(),
                c.getDeliveryMode().name(),
                enrolled,
                c.getTeachers().stream()
                        .map(t -> t.getFullName() != null ? t.getFullName() : t.getUsername())
                        .sorted()
                        .toList()
        );
    }
}
