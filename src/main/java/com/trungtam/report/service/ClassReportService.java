package com.trungtam.report.service;

import com.trungtam.report.dto.response.BreakdownResponse;
import com.trungtam.report.dto.response.ClassAttendanceReport;
import com.trungtam.report.dto.response.ClassExamAverageItem;
import com.trungtam.report.dto.response.ClassStudentAverageItem;
import com.trungtam.report.dto.response.TopicMasteryItem;
import com.trungtam.report.repository.ReportAggregationRepository;
import com.trungtam.report.repository.ReportAggregationRepository.AttendanceRateProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassReportService {

    private final ReportAggregationRepository aggregationRepository;

    public List<ClassExamAverageItem> examAverages(Long classId) {
        return aggregationRepository.examAveragesForClass(classId).stream()
                .map(p -> new ClassExamAverageItem(
                        p.getExamId(), p.getExamName(), ReportMapper.instant(p.getPublishAt()),
                        ReportMapper.scale(p.getAvgScore()), ReportMapper.scale(p.getMaxScore()),
                        ReportMapper.nz(p.getSubmittedCount()), ReportMapper.nz(p.getAssignedCount())))
                .toList();
    }

    public BreakdownResponse breakdowns(Long classId) {
        return ReportMapper.breakdown(
                aggregationRepository.difficultyBreakdown(null, classId, null),
                aggregationRepository.typeBreakdown(null, classId, null));
    }

    public List<TopicMasteryItem> topicMastery(Long classId) {
        return aggregationRepository.topicMastery(null, classId).stream()
                .map(ReportMapper::topic)
                .toList();
    }

    public ClassAttendanceReport attendance(Long classId) {
        return new ClassAttendanceReport(
                ReportMapper.attendance(aggregationRepository.attendanceForClass(classId)),
                aggregationRepository.attendanceByMonthForClass(classId).stream()
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
