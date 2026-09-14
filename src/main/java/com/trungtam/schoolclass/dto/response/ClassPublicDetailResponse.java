package com.trungtam.schoolclass.dto.response;

import com.trungtam.schoolclass.entity.ClassMarketingContent;
import com.trungtam.schoolclass.entity.SchoolClass;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Trang chi tiet khoa hoc CONG KHAI (Card -> bam vao mo trang nay) — nguon
 * cho ca khach chua dang nhap, giong quy tac cua {@link ClassCatalogItem}.
 * Chi tra khoa ACTIVE (xem ClassService#getPublicDetail).
 */
public record ClassPublicDetailResponse(
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
        BigDecimal fullPrice,
        String paymentType,
        String deliveryMode,
        List<String> teacherNames,
        /** Tieu de hien thi — null = FE tu dung {@code name} o tren. */
        String title,
        String coverImageUrl,
        String contentMd,
        /** Nguoi dang xem (neu la HS) da o trong lop chua — an nut Dang ky khi da co roi. */
        boolean enrolled
) {
    public static ClassPublicDetailResponse from(SchoolClass c, ClassMarketingContent content, boolean enrolled) {
        return new ClassPublicDetailResponse(
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
                c.getFullPrice(),
                c.getPaymentType().name(),
                c.getDeliveryMode().name(),
                c.getTeachers().stream()
                        .map(t -> t.getFullName() != null ? t.getFullName() : t.getUsername())
                        .sorted()
                        .toList(),
                content != null ? content.getTitle() : null,
                content != null ? content.getCoverImageUrl() : null,
                content != null ? content.getContentMd() : null,
                enrolled
        );
    }
}
