package com.trungtam.report.service;

import com.trungtam.report.dto.response.AttendanceMonthPoint;
import com.trungtam.report.dto.response.AttendanceSummary;
import com.trungtam.report.dto.response.BreakdownResponse;
import com.trungtam.report.dto.response.BucketStat;
import com.trungtam.report.dto.response.ExamScoreDistribution;
import com.trungtam.report.dto.response.RecentExamItem;
import com.trungtam.report.dto.response.ScoreBand;
import com.trungtam.report.dto.response.ScoreTrendPoint;
import com.trungtam.report.dto.response.TopicMasteryItem;
import com.trungtam.report.repository.ReportAggregationRepository.AttendanceMonthProjection;
import com.trungtam.report.repository.ReportAggregationRepository.AttendanceProjection;
import com.trungtam.report.repository.ReportAggregationRepository.BreakdownProjection;
import com.trungtam.report.repository.ReportAggregationRepository.RecentExamProjection;
import com.trungtam.report.repository.ReportAggregationRepository.ScoreBandProjection;
import com.trungtam.report.repository.ReportAggregationRepository.ScoreStatsProjection;
import com.trungtam.report.repository.ReportAggregationRepository.ScoreTrendProjection;
import com.trungtam.report.repository.ReportAggregationRepository.TopicMasteryProjection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
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

    /**
     * Dung ExamScoreDistribution (§12) tu ket qua query. maxScore <= 0 -> pho diem rong.
     * studentBandIndex khop width_bucket + LEAST clamp; percentile = leCount/submitted*100.
     */
    static ExamScoreDistribution distribution(Long examId, String examName, BigDecimal maxScore,
                                              int bandCount, List<ScoreBandProjection> bandRows,
                                              ScoreStatsProjection stats, Integer classSize,
                                              Integer rank) {
        BigDecimal max = maxScore == null ? BigDecimal.ZERO : maxScore;
        BigDecimal studentScore = stats == null ? null : stats.getStudentScore();

        Integer studentBandIndex = null;
        if (studentScore != null && max.signum() > 0) {
            int raw = (int) Math.floor(studentScore.doubleValue() / max.doubleValue() * bandCount) + 1;
            studentBandIndex = Math.min(Math.max(raw, 1), bandCount);
        }
        final Integer sbi = studentBandIndex;

        List<ScoreBand> bands = bandRows.stream().map(b -> {
            int i = b.getIndex();
            BigDecimal from = max.multiply(BigDecimal.valueOf(i - 1L))
                    .divide(BigDecimal.valueOf(bandCount), 2, RoundingMode.HALF_UP);
            BigDecimal to = max.multiply(BigDecimal.valueOf(i))
                    .divide(BigDecimal.valueOf(bandCount), 2, RoundingMode.HALF_UP);
            return new ScoreBand(i, from, to, nz(b.getCount()), sbi != null && sbi == i);
        }).toList();

        long submitted = stats == null ? 0 : nz(stats.getSubmittedCount());
        Double percentile = null;
        if (studentScore != null && submitted > 0) {
            percentile = BigDecimal.valueOf(nz(stats.getLeCount()) * 100.0 / submitted)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();
        }
        BigDecimal median = stats == null || stats.getMedian() == null
                ? null
                : BigDecimal.valueOf(stats.getMedian()).setScale(2, RoundingMode.HALF_UP);

        return new ExamScoreDistribution(
                examId, examName, max, bandCount, bands,
                studentScore, sbi, percentile,
                stats == null ? null : scale(stats.getAvgScore()),
                median,
                stats == null ? null : stats.getHighest(),
                stats == null ? null : stats.getLowest(),
                rank, (int) submitted, classSize);
    }

    /**
     * Pho diem TONG cua khoa (§12.2): bucket cac gia tri 0..10 (diem TB HV quy
     * ve thang 10) thanh bandCount khoang; danh dau khoang chua HV neu co.
     */
    static ExamScoreDistribution spectrum(String name, List<Double> values,
                                          Double studentValue, int bandCount,
                                          Integer classSize) {
        double max = 10.0;
        long[] counts = new long[bandCount];
        for (double v : values) {
            counts[bandOf(v, max, bandCount) - 1]++;
        }
        Integer sbi = studentValue == null ? null : bandOf(studentValue, max, bandCount);

        List<ScoreBand> bands = new ArrayList<>();
        for (int i = 1; i <= bandCount; i++) {
            BigDecimal from = BigDecimal.valueOf(max * (i - 1) / bandCount).setScale(2, RoundingMode.HALF_UP);
            BigDecimal to = BigDecimal.valueOf(max * i / bandCount).setScale(2, RoundingMode.HALF_UP);
            bands.add(new ScoreBand(i, from, to, counts[i - 1], sbi != null && sbi == i));
        }

        int total = values.size();
        Double percentile = null;
        if (studentValue != null && total > 0) {
            long le = values.stream().filter(v -> v <= studentValue).count();
            percentile = BigDecimal.valueOf(le * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
        }
        BigDecimal avg = null;
        BigDecimal median = null;
        BigDecimal highest = null;
        BigDecimal lowest = null;
        if (!values.isEmpty()) {
            List<Double> sorted = values.stream().sorted().toList();
            avg = BigDecimal.valueOf(values.stream().mapToDouble(x -> x).average().orElse(0))
                    .setScale(2, RoundingMode.HALF_UP);
            median = BigDecimal.valueOf(sorted.get(sorted.size() / 2)).setScale(2, RoundingMode.HALF_UP);
            highest = BigDecimal.valueOf(sorted.get(sorted.size() - 1)).setScale(2, RoundingMode.HALF_UP);
            lowest = BigDecimal.valueOf(sorted.get(0)).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal studentScore = studentValue == null
                ? null : BigDecimal.valueOf(studentValue).setScale(2, RoundingMode.HALF_UP);

        return new ExamScoreDistribution(null, name, BigDecimal.TEN, bandCount, bands,
                studentScore, sbi, percentile, avg, median, highest, lowest, null, total, classSize);
    }

    private static int bandOf(double v, double max, int bandCount) {
        int idx = (int) Math.floor(v / max * bandCount) + 1;
        return Math.min(Math.max(idx, 1), bandCount);
    }
}
