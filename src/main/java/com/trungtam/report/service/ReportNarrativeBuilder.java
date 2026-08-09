package com.trungtam.report.service;

import com.trungtam.report.dto.response.AttendanceSummary;
import com.trungtam.report.repository.ReportAggregationRepository.StudentAvgProjection;
import com.trungtam.report.repository.ReportAggregationRepository.TypeDifficultyProjection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sinh cau chu tieng Viet cho Bang "Phan tich tu dong" (Analysis Card) — 1
 * lan duy nhat o BE, tranh lap logic nguong o ca ADMIN (TypeScript) lan
 * Mobile (Dart). Ham thuan: input so -> output String, khong query DB.
 */
@Component
class ReportNarrativeBuilder {

    private static final Map<String, String> TYPE_LABEL = Map.of(
            "MULTIPLE_CHOICE", "Trắc nghiệm",
            "TRUE_FALSE", "Đúng-Sai",
            "ESSAY", "Tự luận");
    private static final Map<String, String> DIFFICULTY_LABEL = Map.of(
            "EASY", "Dễ",
            "MEDIUM", "Trung bình",
            "HARD", "Khó");
    private static final List<String> TYPE_ORDER =
            List.of("MULTIPLE_CHOICE", "TRUE_FALSE", "ESSAY");
    private static final List<String> DIFFICULTY_ORDER =
            List.of("EASY", "MEDIUM", "HARD");

    @Value("${app.practice.weak-threshold:0.6}")
    private double weakThreshold;
    @Value("${app.practice.min-sample:3}")
    private int minSample;
    @Value("${app.report.comparison-threshold:0.05}")
    private double comparisonThreshold;

    /**
     * Khoang diem TB (band) co nhieu HV nhat trong lop, bucket tren thang 10
     * diem (avgPct * 10) thanh 10 khoang [0-1), [1-2), ..., [9-10]. Nguon
     * PHAI la diem TB toan khoa cua tung HV (studentAveragesForClass) — KHONG
     * dung courseSpectrum vi do la du lieu theo tung bai thi, khac "pho diem
     * trung binh" ma mockup mo ta.
     */
    String scoreSpectrumLabel(List<StudentAvgProjection> allStudents) {
        int[] counts = new int[10];
        boolean any = false;
        for (StudentAvgProjection p : allStudents) {
            if (p.getAvgPct() == null) continue;
            double value = p.getAvgPct() * 10.0;
            int band = Math.min(9, Math.max(0, (int) Math.floor(value)));
            counts[band]++;
            any = true;
        }
        if (!any) return null;
        int maxBand = 0;
        for (int i = 1; i < 10; i++) {
            if (counts[i] > counts[maxBand]) maxBand = i;
        }
        return maxBand + " - " + (maxBand + 1) + " điểm";
    }

    /** DENSE_RANK giam dan theo diem — diem bang nhau xep chung hang. */
    Map<Long, Integer> denseRankDesc(Map<Long, BigDecimal> scores) {
        List<BigDecimal> sorted = new ArrayList<>(scores.values());
        sorted.sort((a, b) -> b.compareTo(a));
        Map<BigDecimal, Integer> rankByScore = new LinkedHashMap<>();
        int rank = 0;
        BigDecimal prev = null;
        for (BigDecimal s : sorted) {
            if (prev == null || s.compareTo(prev) != 0) {
                rank++;
                prev = s;
            }
            rankByScore.putIfAbsent(s, rank);
        }
        Map<Long, Integer> result = new LinkedHashMap<>();
        scores.forEach((studentId, score) -> result.put(studentId, rankByScore.get(score)));
        return result;
    }

    /**
     * Nhan dinh nang luc theo TUNG loai cau (TN/DS/TL), tong hop tren MOI muc
     * do kho. Loai khong du mau o TAT CA muc do -> bo qua (khong doan mo,
     * nhat quan voi {@code weak()} trong PracticeAssignmentService).
     */
    List<String> abilityInsights(List<TypeDifficultyProjection> rows) {
        Map<String, Map<String, TypeDifficultyProjection>> byType = new LinkedHashMap<>();
        for (TypeDifficultyProjection r : rows) {
            byType.computeIfAbsent(r.getExamType(), k -> new LinkedHashMap<>())
                    .put(r.getDifficulty(), r);
        }

        List<String> insights = new ArrayList<>();
        for (String type : TYPE_ORDER) {
            Map<String, TypeDifficultyProjection> byDifficulty = byType.get(type);
            if (byDifficulty == null) continue;

            if ("ESSAY".equals(type)) {
                long graded = byDifficulty.values().stream()
                        .mapToLong(r -> nz(r.getCorrectCount()) + nz(r.getIncorrectCount())).sum();
                if (graded == 0) {
                    insights.add("Dạng bài Tự luận: chưa có dữ liệu chấm");
                    continue;
                }
                // Co du lieu da cham (khong phai toan bo ungraded) -> roi xuong
                // phan tich tot/yeu binh thuong nhu MC/TF, KHONG bo qua.
            }

            List<String> good = new ArrayList<>();
            List<String> weak = new ArrayList<>();
            for (String difficulty : DIFFICULTY_ORDER) {
                TypeDifficultyProjection r = byDifficulty.get(difficulty);
                if (r == null) continue;
                long graded = nz(r.getCorrectCount()) + nz(r.getIncorrectCount());
                if (graded < minSample) continue;
                double pct = (double) nz(r.getCorrectCount()) / graded;
                (pct >= weakThreshold ? good : weak).add(DIFFICULTY_LABEL.get(difficulty));
            }
            if (good.isEmpty() && weak.isEmpty()) continue;

            StringBuilder sb = new StringBuilder("Dạng bài ").append(TYPE_LABEL.get(type)).append(": ");
            List<String> parts = new ArrayList<>();
            if (!good.isEmpty()) parts.add("làm tốt mức độ " + String.join(", ", good));
            if (!weak.isEmpty()) parts.add("cần cải thiện mức độ " + String.join(", ", weak));
            sb.append(String.join("; ", parts));
            insights.add(sb.toString());
        }
        return insights;
    }

    /**
     * Nhan dinh chuyen can — uu tien dung gio truoc, roi den di hoc dau dan.
     * Luon kem so lieu cu the (so buoi + % ) thay vi chi nhan xet chung chung,
     * de PH/GV thay ngay muc do nghiem trong thay vi phai tu doi chieu.
     */
    String attendanceInsight(AttendanceSummary s) {
        if (s.attendanceRate() == null) {
            return null;
        }
        String stats = attendanceStats(s);
        if (s.onTimeRate() != null && s.onTimeRate() < weakThreshold) {
            return "Thường xuyên đi học muộn — " + stats;
        }
        if (s.attendanceRate() < weakThreshold) {
            return "Hay nghỉ học, cần chú ý chuyên cần — " + stats;
        }
        return "Đi học đều đặn và đúng giờ — " + stats;
    }

    /** "12/15 buổi có mặt (80%), đúng giờ 92%" — so lieu tho cho attendanceInsight. */
    private static String attendanceStats(AttendanceSummary s) {
        long present = s.coMat() + s.tre();
        StringBuilder sb = new StringBuilder();
        sb.append(present).append("/").append(s.totalSessions()).append(" buổi có mặt");
        if (s.attendanceRate() != null) {
            sb.append(" (").append(Math.round(s.attendanceRate() * 100)).append("%)");
        }
        if (s.onTimeRate() != null) {
            sb.append(", đúng giờ ").append(Math.round(s.onTimeRate() * 100)).append("%");
        }
        if (s.vang() > 0) {
            sb.append(", vắng ").append(s.vang()).append(" buổi");
        }
        return sb.toString();
    }

    /** "Chương I", "Chương II", ... theo thu tu sortOrder — KHONG dung ten chuong that (mockup dung so La Ma). */
    String chapterLabel(int index) {
        return "Chương " + toRoman(index);
    }

    /**
     * So sanh 1 diem voi TB lop — dung cho Buoi hoc + Bai thi (§ Phan C).
     * Nguong 5% (KHAC weakThreshold 60% dung cho dung/sai) de tranh cau
     * "cao hon 0.4%" vo nghia — day la % CHENH LECH tuong doi so voi TB lop,
     * khong phai ti le dung.
     */
    String comparisonInsight(BigDecimal score, BigDecimal classAverage) {
        if (score == null || classAverage == null || classAverage.signum() == 0) {
            return null;
        }
        double pct = score.subtract(classAverage).doubleValue() / classAverage.doubleValue();
        if (pct > comparisonThreshold) {
            return "Điểm cao hơn " + formatPct(pct) + "% so với TB lớp";
        }
        if (pct < -comparisonThreshold) {
            return "Điểm thấp hơn " + formatPct(-pct) + "% so với TB lớp";
        }
        return "Điểm xấp xỉ TB lớp";
    }

    private static String formatPct(double pct) {
        return BigDecimal.valueOf(pct * 100).setScale(1, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static final int[] ROMAN_VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] ROMAN_SYMBOLS =
            {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private static String toRoman(int n) {
        StringBuilder sb = new StringBuilder();
        int remaining = n;
        for (int i = 0; i < ROMAN_VALUES.length && remaining > 0; i++) {
            while (remaining >= ROMAN_VALUES[i]) {
                remaining -= ROMAN_VALUES[i];
                sb.append(ROMAN_SYMBOLS[i]);
            }
        }
        return sb.toString();
    }
}
