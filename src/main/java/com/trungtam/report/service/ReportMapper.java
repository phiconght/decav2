package com.trungtam.report.service;

import com.trungtam.report.dto.response.AttendanceMonthPoint;
import com.trungtam.report.dto.response.AttendanceSummary;
import com.trungtam.report.dto.response.BreakdownResponse;
import com.trungtam.report.dto.response.BucketStat;
import com.trungtam.report.dto.response.RecentExamItem;
import com.trungtam.report.dto.response.ScoreTrendPoint;
import com.trungtam.report.dto.response.TopicMasteryItem;
import com.trungtam.report.repository.ReportAggregationRepository.AttendanceMonthProjection;
import com.trungtam.report.repository.ReportAggregationRepository.AttendanceProjection;
import com.trungtam.report.repository.ReportAggregationRepository.BreakdownProjection;
import com.trungtam.report.repository.ReportAggregationRepository.RecentExamProjection;
import com.trungtam.report.repository.ReportAggregationRepository.ScoreTrendProjection;
import com.trungtam.report.repository.ReportAggregationRepository.TopicMasteryProjection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/** Chuyen projection (native query) sang DTO + tinh cac ti le dan xuat. */
final class ReportMapper {

    private ReportMapper() {
    }

    static Instant instant(Instant ts) {
        return ts;
    }

    static long nz(Long v) {
        return v == null ? 0L : v;
    }

    static RecentExamItem recentExam(RecentExamProjection p) {
        return new RecentExamItem(
                p.getExamStudentId(), p.getExamId(), p.getExamCode(), p.getExamName(),
                p.getSubjectName(), p.getClassId(), p.getClassName(),
                instant(p.getSubmittedAt()), p.getScore(), p.getMaxScore());
    }

    static ScoreTrendPoint trendPoint(ScoreTrendProjection p) {
        return new ScoreTrendPoint(
                p.getExamId(), p.getExamName(), instant(p.getPublishAt()), instant(p.getSubmittedAt()),
                p.getScore(), p.getMaxScore(), scale(p.getClassAverage()));
    }

    static BucketStat bucket(BreakdownProjection p) {
        long correct = nz(p.getCorrectCount());
        long incorrect = nz(p.getIncorrectCount());
        long ungraded = nz(p.getUngradedCount());
        Double pct = (correct + incorrect) == 0
                ? null
                : round((double) correct / (correct + incorrect));
        return new BucketStat(p.getBucketKey(), correct, incorrect, ungraded, pct);
    }

    /** Bao dam du 3 bucket do kho / loai cau (bucket thieu = so 0). */
    static BreakdownResponse breakdown(List<BreakdownProjection> byDifficulty,
                                       List<BreakdownProjection> byType) {
        return new BreakdownResponse(
                fill(byDifficulty, List.of("EASY", "MEDIUM", "HARD")),
                fill(byType, List.of("MULTIPLE_CHOICE", "TRUE_FALSE", "ESSAY")));
    }

    private static List<BucketStat> fill(List<BreakdownProjection> rows, List<String> keys) {
        List<BucketStat> present = rows.stream().map(ReportMapper::bucket).toList();
        return keys.stream()
                .map(k -> present.stream().filter(b -> k.equals(b.key())).findFirst()
                        .orElse(new BucketStat(k, 0, 0, 0, null)))
                .toList();
    }

    static TopicMasteryItem topic(TopicMasteryProjection p) {
        BigDecimal earned = p.getEarned() == null ? BigDecimal.ZERO : p.getEarned();
        BigDecimal max = p.getMax() == null ? BigDecimal.ZERO : p.getMax();
        Double pct = max.signum() == 0
                ? null
                : round(earned.doubleValue() / max.doubleValue());
        return new TopicMasteryItem(
                p.getTopicId(),
                p.getTopicName() == null ? "Chưa phân chương" : p.getTopicName(),
                nz(p.getGradedCount()), nz(p.getCorrectCount()), nz(p.getUngradedCount()),
                earned, max, pct);
    }

    static AttendanceSummary attendance(AttendanceProjection p) {
        if (p == null) {
            return new AttendanceSummary(0, 0, 0, 0, 0, 0, null, null);
        }
        long total = nz(p.getTotalSessions());
        long coMat = nz(p.getCoMat());
        long tre = nz(p.getTre());
        long vang = nz(p.getVang());
        long coPhep = nz(p.getCoPhep());
        long chuaCheckin = nz(p.getChuaCheckin());
        Double attendanceRate = total == 0 ? null : round((double) (coMat + tre) / total);
        Double onTimeRate = (coMat + tre) == 0 ? null : round((double) coMat / (coMat + tre));
        return new AttendanceSummary(total, coMat, tre, vang, coPhep, chuaCheckin,
                attendanceRate, onTimeRate);
    }

    static AttendanceMonthPoint month(AttendanceMonthProjection p) {
        return new AttendanceMonthPoint(p.getMonth(),
                nz(p.getCoMat()), nz(p.getTre()), nz(p.getVang()), nz(p.getCoPhep()));
    }

    static Double round(double v) {
        return BigDecimal.valueOf(v).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    static BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }
}
