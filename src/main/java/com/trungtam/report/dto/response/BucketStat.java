package com.trungtam.report.dto.response;

/**
 * Thong ke dung/sai/cho-cham cho 1 nhom (do kho hoac loai cau).
 * key = EASY|MEDIUM|HARD hoac MULTIPLE_CHOICE|ESSAY|TRUE_FALSE.
 * correctPct = correct/(correct+incorrect), null neu mau so 0 (toan cho cham).
 */
public record BucketStat(
        String key,
        long correctCount,
        long incorrectCount,
        long ungradedCount,
        Double correctPct
) {
}
