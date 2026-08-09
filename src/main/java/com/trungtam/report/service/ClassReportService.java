package com.trungtam.report.service;

import com.trungtam.report.dto.response.BreakdownResponse;
import com.trungtam.report.dto.response.ClassAttendanceReport;
import com.trungtam.report.dto.response.ClassExamAverageItem;
import com.trungtam.report.dto.response.ClassStudentAverageItem;
import com.trungtam.report.dto.response.ExamScoreDistribution;
import com.trungtam.report.dto.response.TopicMasteryItem;
import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.report.repository.ReportAggregationRepository;
import com.trungtam.report.repository.ReportAggregationRepository.AttendanceRateProjection;
import com.trungtam.report.repository.ReportAggregationRepository.ScoreBandProjection;
import com.trungtam.report.repository.ReportAggregationRepository.ScoreStatsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassReportService {

    private final ReportAggregationRepository aggregationRepository;
    private final ExamRepository examRepository;
    private final com.trungtam.schedule.repository.ClassSessionRepository classSessionRepository;

    @Value("${app.report.score-band-count:10}")
    private int bandCount;

    public List<ClassExamAverageItem> examAverages(Long classId, Long topicId) {
        return aggregationRepository.examAveragesForClass(classId, topicId).stream()
                .map(p -> new ClassExamAverageItem(
                        p.getExamId(), p.getExamName(), ReportMapper.instant(p.getPublishAt()),
                        ReportMapper.scale(p.getAvgScore()), ReportMapper.scale(p.getMaxScore()),
                        ReportMapper.nz(p.getSubmittedCount()), ReportMapper.nz(p.getAssignedCount())))
                .toList();
    }

    /**
     * Breakdown ca lop theo chuong (topicId nullable = toan khoa) hoac theo
     * buoi hoc (sessionId nullable). §11 + bao cao cap buoi.
     */
    public BreakdownResponse breakdowns(Long classId, Long topicId, Long sessionId) {
        return ReportMapper.breakdown(
                aggregationRepository.difficultyBreakdown(null, classId, topicId, sessionId, null),
                aggregationRepository.typeBreakdown(null, classId, topicId, sessionId, null));
    }

    /** Bao cao cap buoi hoc cho ca lop (§ Bao cao 3 cap do). */
    public List<ClassExamAverageItem> sessionExams(Long classId, Long sessionId) {
        requireSessionInClass(sessionId, classId);
        return aggregationRepository.sessionExamAveragesForClass(classId, sessionId).stream()
                .map(p -> new ClassExamAverageItem(
                        p.getExamId(), p.getExamName(), ReportMapper.instant(p.getPublishAt()),
                        ReportMapper.scale(p.getAvgScore()), ReportMapper.scale(p.getMaxScore()),
                        ReportMapper.nz(p.getSubmittedCount()), ReportMapper.nz(p.getAssignedCount())))
                .toList();
    }

    public BreakdownResponse sessionBreakdowns(Long classId, Long sessionId) {
        requireSessionInClass(sessionId, classId);
        return breakdowns(classId, null, sessionId);
    }

    private void requireSessionInClass(Long sessionId, Long classId) {
        if (!classSessionRepository.existsByIdAndClazzId(sessionId, classId)) {
            throw new com.trungtam.common.exception.AppException(
                    com.trungtam.common.exception.ErrorCode.SESSION_NOT_IN_CLASS);
        }
    }

    /** Pho diem TONG cua khoa (§12.2) — bucket diem TB HV, khong danh dau. */
    public ExamScoreDistribution courseSpectrum(Long classId, int bandCount) {
        java.util.List<Double> values = new java.util.ArrayList<>();
        for (var r : aggregationRepository.studentAveragesForClass(classId)) {
            if (r.getAvgPct() != null) {
                values.add(r.getAvgPct() * 10.0);
            }
        }
        return ReportMapper.spectrum("Điểm trung bình khóa", values, null,
                Math.max(5, Math.min(bandCount, 60)), null);
    }

    /** Pho diem 1 bai thi cua ca lop (§12.1) — khong danh dau HV. bandCount tuy chon. */
    public ExamScoreDistribution scoreDistribution(Long classId, Long examId, int nBands) {
        var exam = examRepository.findById(examId)
                .orElseThrow(() -> new com.trungtam.common.exception.AppException(
                        com.trungtam.common.exception.ErrorCode.EXAM_NOT_FOUND));
        boolean examInClass = exam.getClasses().stream().anyMatch(c -> c.getId().equals(classId));
        if (!examInClass) {
            throw new com.trungtam.common.exception.AppException(
                    com.trungtam.common.exception.ErrorCode.EXAM_NOT_IN_CLASS);
        }
        int n = Math.max(5, Math.min(nBands, 60));
        BigDecimal maxScore = aggregationRepository.examMaxScore(examId);
        String examName = exam.getName();
        List<ScoreBandProjection> bands = maxScore != null && maxScore.signum() > 0
                ? aggregationRepository.scoreDistribution(examId, classId, maxScore, n)
                : List.of();
        ScoreStatsProjection stats = aggregationRepository.scoreStats(examId, classId, null);
        return ReportMapper.distribution(examId, examName, maxScore, n, bands,
                stats, null, null);
    }

    public List<TopicMasteryItem> topicMastery(Long classId) {
        return aggregationRepository.topicMastery(null, classId).stream()
                .map(ReportMapper::topic)
                .toList();
    }

    public ClassAttendanceReport attendance(Long classId, Long topicId) {
        return new ClassAttendanceReport(
                ReportMapper.attendance(aggregationRepository.attendanceForClass(classId, topicId)),
                aggregationRepository.attendanceByMonthForClass(classId, topicId).stream()
                        .map(ReportMapper::month).toList());
    }

    public List<ClassStudentAverageItem> students(Long classId) {
        Map<Long, Double> rateByStudent = aggregationRepository.attendanceRatePerStudent(classId).stream()
                .collect(Collectors.toMap(
                        AttendanceRateProjection::getStudentId,
                        this::attendanceRate));

        return aggregationRepository.studentAveragesForClass(classId).stream()
                .map(p -> new ClassStudentAverageItem(
                        p.getStudentId(), p.getFullName(), p.getUsername(),
                        ReportMapper.nz(p.getSubmittedCount()),
                        ReportMapper.scale(p.getAvgScore()),
                        p.getAvgPct() == null ? null : ReportMapper.round(p.getAvgPct()),
                        rateByStudent.get(p.getStudentId())))
                .toList();
    }

    private Double attendanceRate(AttendanceRateProjection r) {
        long total = ReportMapper.nz(r.getTotal());
        if (total == 0) {
            return null;
        }
        long present = ReportMapper.nz(r.getCoMat()) + ReportMapper.nz(r.getTre());
        return ReportMapper.round((double) present / total);
    }
}
