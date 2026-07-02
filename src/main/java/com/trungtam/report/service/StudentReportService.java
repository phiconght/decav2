package com.trungtam.report.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.entity.ExamStudent;
import com.trungtam.exam.repository.ExamQuestionResultRepository;
import com.trungtam.exam.repository.ExamStudentRepository;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.report.dto.response.BreakdownResponse;
import com.trungtam.report.dto.response.ChildOption;
import com.trungtam.report.dto.response.ExamReportDetail;
import com.trungtam.report.dto.response.ExamScoreDistribution;
import com.trungtam.report.dto.response.RecentExamItem;
import com.trungtam.report.dto.response.ScoreTrendPoint;
import com.trungtam.report.dto.response.StudentAttendanceReport;
import com.trungtam.report.dto.response.StudentClassOption;
import com.trungtam.report.dto.response.StudentClassSummaryResponse;
import com.trungtam.report.dto.response.StudentClassSummaryResponse.ClassInfo;
import com.trungtam.report.dto.response.StudentClassSummaryResponse.StudentInfo;
import com.trungtam.report.dto.response.TopicMasteryItem;
import com.trungtam.exam.entity.Exam;
import com.trungtam.report.repository.ReportAggregationRepository;
import com.trungtam.report.repository.ReportAggregationRepository.RankStatsProjection;
import com.trungtam.report.repository.ReportAggregationRepository.ScoreBandProjection;
import com.trungtam.report.repository.ReportAggregationRepository.ScoreStatsProjection;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentReportService {

    private final ReportAggregationRepository aggregationRepository;
    private final ExamStudentRepository examStudentRepository;
    private final ExamQuestionResultRepository examQuestionResultRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final StudentParentRepository studentParentRepository;
    private final UserRepository userRepository;
    private final ReportCommentService reportCommentService;

    /** So khoang chia pho diem (§12). */
    @Value("${app.report.score-band-count:10}")
    private int bandCount;

    public List<RecentExamItem> recentExams(Long studentId, int limit) {
        return aggregationRepository.recentSubmitted(studentId, Math.max(1, limit)).stream()
                .map(ReportMapper::recentExam)
                .toList();
    }

    public List<RecentExamItem> examHistory(Long studentId, Long classId) {
        return aggregationRepository.historyInClass(studentId, classId).stream()
                .map(ReportMapper::recentExam)
                .toList();
    }

    public ExamReportDetail examDetail(Long studentId, Long examId, Long classId) {
        requireStudentInClass(studentId, classId);
        ExamStudent es = examStudentRepository.findByExamIdAndUserId(examId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_AVAILABLE));
        boolean examInClass = es.getExam().getClasses().stream()
                .anyMatch(c -> c.getId().equals(classId));
        if (!examInClass) {
            throw new AppException(ErrorCode.EXAM_NOT_IN_CLASS);
        }
        BigDecimal maxScore = examQuestionResultRepository.sumMaxPointsByExamStudentId(es.getId());
        RankStatsProjection stats = aggregationRepository.rankAndStats(examId, classId, studentId);
        int classSize = (int) schoolClassRepository.countStudents(classId);

        // NANG LUC theo CHUONG cua de (§11): de khong gan chuong -> toan khoa (topicId=null).
        Exam exam = es.getExam();
        Long topicId = exam.getTopic() == null ? null : exam.getTopic().getId();
        String topicName = exam.getTopic() == null ? null : exam.getTopic().getName();
        BreakdownResponse breakdown = breakdowns(studentId, classId, topicId);

        // PHO DIEM cua bai thi (§12) — 1 call gop san.
        ExamScoreDistribution distribution =
                buildDistribution(examId, exam.getName(), classId, studentId);

        return new ExamReportDetail(
                examId,
                exam.getName(),
                exam.getCode(),
                es.getSubmittedAt(),
                es.getScore(),
                maxScore,
                stats == null ? null : ReportMapper.scale(stats.getClassAverage()),
                stats == null || stats.getRank() == null ? null : stats.getRank().intValue(),
                stats == null ? null : (int) ReportMapper.nz(stats.getSubmittedCount()),
                classSize,
                topicId,
                topicName,
                breakdown,
                distribution);
    }

    public List<ScoreTrendPoint> scoreTrend(Long studentId, Long classId) {
        return aggregationRepository.trendForStudentInClass(studentId, classId).stream()
                .map(ReportMapper::trendPoint)
                .toList();
    }

    /** Breakdown NANG LUC theo chuong (topicId nullable = toan khoa). §11. */
    public BreakdownResponse breakdowns(Long studentId, Long classId, Long topicId) {
        return ReportMapper.breakdown(
                aggregationRepository.difficultyBreakdown(studentId, classId, topicId),
                aggregationRepository.typeBreakdown(studentId, classId, topicId));
    }

    /** Pho diem 1 bai thi cho 1 HV (§12.1) — co danh dau vi tri HV. */
    public ExamScoreDistribution scoreDistribution(Long studentId, Long examId, Long classId) {
        requireStudentInClass(studentId, classId);
        String examName = examStudentRepository.findByExamIdAndUserId(examId, studentId)
                .map(es -> es.getExam().getName())
                .orElse(null);
        return buildDistribution(examId, examName, classId, studentId);
    }

    private ExamScoreDistribution buildDistribution(Long examId, String examName,
                                                    Long classId, Long studentId) {
        BigDecimal maxScore = aggregationRepository.examMaxScore(examId);
        int classSize = (int) schoolClassRepository.countStudents(classId);
        Integer rank = null;
        if (studentId != null) {
            RankStatsProjection rs = aggregationRepository.rankAndStats(examId, classId, studentId);
            rank = rs == null || rs.getRank() == null ? null : rs.getRank().intValue();
        }
        List<ScoreBandProjection> bands = maxScore != null && maxScore.signum() > 0
                ? aggregationRepository.scoreDistribution(examId, classId, maxScore, bandCount)
                : List.of();
        ScoreStatsProjection stats = aggregationRepository.scoreStats(examId, classId, studentId);
        return ReportMapper.distribution(examId, examName, maxScore, bandCount, bands,
                stats, classSize, rank);
    }

    public List<TopicMasteryItem> topicMastery(Long studentId, Long classId) {
        return aggregationRepository.topicMastery(studentId, classId).stream()
                .map(ReportMapper::topic)
                .toList();
    }

    public StudentAttendanceReport attendance(Long studentId, Long classId) {
        return new StudentAttendanceReport(
                ReportMapper.attendance(aggregationRepository.attendanceForStudent(studentId, classId)),
                aggregationRepository.attendanceByMonthForStudent(studentId, classId).stream()
                        .map(ReportMapper::month).toList());
    }

    public List<StudentClassOption> listClasses(Long studentId) {
        return schoolClassRepository.findClassesByStudentId(studentId).stream()
                .map(c -> new StudentClassOption(
                        c.getId(), c.getCode(), c.getName(),
                        c.getSubject().getName() + " — " + c.getSubject().getGradeLevel(),
                        teacherNames(c)))
                .toList();
    }

    /** Khoa hoc cho HUB bao cao: ADMIN/EMPLOYEE = tat ca; TEACHER/ASSISTANT = lop minh day. */
    public List<StudentClassOption> myClasses() {
        java.util.Set<String> roles = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        List<SchoolClass> classes;
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE")) {
            classes = schoolClassRepository.findAll();
        } else {
            classes = schoolClassRepository.findClassesByTeacherId(currentUserId());
        }
        return classes.stream()
                .map(c -> new StudentClassOption(
                        c.getId(), c.getCode(), c.getName(),
                        c.getSubject().getName() + " — " + c.getSubject().getGradeLevel(),
                        teacherNames(c)))
                .toList();
    }

    public List<ChildOption> myChildren() {
        Long parentId = currentUserId();
        return studentParentRepository.findByParentIdOrderByIdAsc(parentId).stream()
                .map(sp -> new ChildOption(
                        sp.getStudent().getId(),
                        sp.getStudent().getFullName(),
                        sp.getStudent().getUsername()))
                .toList();
    }

    public StudentClassSummaryResponse summary(Long studentId, Long classId) {
        requireStudentInClass(studentId, classId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        SchoolClass clazz = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));

        return new StudentClassSummaryResponse(
                new StudentInfo(student.getId(), student.getFullName(), student.getUsername(),
                        student.getEmail(), student.getPhone()),
                new ClassInfo(clazz.getId(), clazz.getCode(), clazz.getName(),
                        clazz.getSubject().getName() + " — " + clazz.getSubject().getGradeLevel(),
                        teacherNames(clazz)),
                examHistory(studentId, classId),
                scoreTrend(studentId, classId),
                breakdowns(studentId, classId, null),
                topicMastery(studentId, classId),
                attendance(studentId, classId),
                reportCommentService.list(studentId, classId, null));
    }

    // ---- helpers ----

    private void requireStudentInClass(Long studentId, Long classId) {
        if (!schoolClassRepository.existsByIdAndStudents_Id(classId, studentId)) {
            throw new AppException(ErrorCode.STUDENT_NOT_IN_CLASS);
        }
    }

    private String teacherNames(SchoolClass c) {
        return c.getTeachers().stream()
                .map(User::getFullName)
                .collect(Collectors.joining(", "));
    }

    private Long currentUserId() {
        return userRepository.findByUsername(SecurityUtils.requireCurrentUsername())
                .map(User::getId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
