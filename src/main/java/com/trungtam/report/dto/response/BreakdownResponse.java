package com.trungtam.report.dto.response;

import java.util.List;

/** Breakdown dung/sai theo do kho + theo loai cau. */
public record BreakdownResponse(
        List<BucketStat> byDifficulty,
        List<BucketStat> byType
) {
}
