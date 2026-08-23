package com.trungtam.marketing.dto.response;

import com.trungtam.marketing.entity.TrustStats;

public record TrustStatsResponse(
        String years,
        String students,
        String teachers
) {
    public static TrustStatsResponse from(TrustStats t) {
        return new TrustStatsResponse(t.getYears(), t.getStudents(), t.getTeachers());
    }

    public static TrustStatsResponse empty() {
        return new TrustStatsResponse("", "", "");
    }
}
