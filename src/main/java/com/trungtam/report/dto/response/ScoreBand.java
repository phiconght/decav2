package com.trungtam.report.dto.response;

import java.math.BigDecimal;

/** 1 khoang diem trong pho diem (histogram). */
public record ScoreBand(
        int index,
        BigDecimal fromScore,
        BigDecimal toScore,
        long count,
        boolean containsStudent
) {
}
