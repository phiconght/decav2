package com.trungtam.schoolclass.dto.request;

import com.trungtam.schoolclass.entity.ClassStatus;
import com.trungtam.schoolclass.entity.DeliveryMode;
import com.trungtam.schoolclass.entity.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateClassRequest(
        @NotBlank String name,
        @NotNull Long subjectId,
        LocalDate startDate,
        LocalDate endDate,
        ClassStatus status,
        List<Long> teacherIds,
        /** Don gia moi buoi (VND). Null -> giu 0 (SPEC_ThanhToan §0.2#1). */
        @PositiveOrZero BigDecimal pricePerSession,
        /** Gia Xu de HS tu dang ky (Mobile/Web). Null/0 -> khong mo ban qua Xu. */
        @PositiveOrZero Long coinPrice,
        /** Gia tron goi cho dang ky tu phuc vu bang chuyen khoan. Null -> an nut Dang ky + QR. */
        @PositiveOrZero BigDecimal fullPrice,
        /** Hinh thuc thanh toan hoc phi. Null -> giu PREPAID_COIN. */
        PaymentType paymentType,
        /** Hinh thuc hoc (quyet dinh cach diem danh). Null -> giu OFFLINE. */
        DeliveryMode deliveryMode
) {}
