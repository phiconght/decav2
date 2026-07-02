package com.trungtam.report.dto.response;

import java.math.BigDecimal;

/**
 * Muc nam chac 1 chuong (Topic). masteryPct = earned/max tren cau da cham
 * (correct != null). topicId = null -> "Chua phan chuong".
 */
public record TopicMasteryItem(
        Long topicId,
        String topicName,
        long gradedCount,
        long correctCount,
        long ungradedCount,
        BigDecimal earned,
        BigDecimal max,
        Double masteryPct
) {
}
