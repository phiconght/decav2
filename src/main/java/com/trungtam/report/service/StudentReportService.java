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
import com.trungtam.report.dto.response.ChapterAnalysisResponse;
import com.trungtam.report.dto.response.ChildOption;
import com.trungtam.report.dto.response.ExamAnalysisResponse;
import com.trungtam.report.dto.response.ExamReportDetail;
import com.trungtam.report.dto.response.ExamScoreDistribution;
import com.trungtam.report.dto.response.RecentExamItem;
import com.trungtam.report.dto.response.ReportAnalysisResponse;
import com.trungtam.report.dto.response.ReportAnalysisResponse.ChapterAnalysisItem;
import com.trungtam.report.dto.response.CommentItem;
import com.trungtam.report.dto.response.ScoreTrendPoint;
import com.trungtam.report.dto.response.SessionAnalysisResponse;
import com.trungtam.report.dto.response.StudentAttendanceReport;
import com.trungtam.report.dto.response.StudentClassOption;
import com.trungtam.report.dto.response.StudentClassSummaryResponse;
import com.trungtam.report.dto.response.StudentClassSummaryResponse.ClassInfo;
import com.trungtam.report.dto.response.StudentClassSummaryResponse.StudentInfo;
import com.trungtam.report.dto.response.TopicMasteryItem;
import com.trungtam.exam.entity.Exam;
import com.trungtam.report.repository.ReportAggregationRepository;
import com.trungtam.report.repository.ReportAggregationRepository.ClassExamAvgProjection;
import com.trungtam.report.repository.ReportAggregationRepository.RankStatsProjection;
import com.trungtam.report.repository.ReportAggregationRepository.ScoreBandProjection;
import com.trungtam.report.repository.ReportAggregationRepository.ScoreStatsProjection;
import com.trungtam.report.repository.ReportAggregationRepository.StudentAvgProjection;
import com.trungtam.report.repository.ReportAggregationRepository.StudentTopicAvgProjection;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final com.trungtam.schedule.repository.ClassSessionRepository classSessionRepository;
    private final ReportNarrativeBuilder narrativeBuilder;

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
        // NGU CANH buoi hoc (§Phan C) — de biet bai thi nay thuoc buoi nao.
        Long sessionId = exam.getSession() == null ? null : exam.getSession().getId();
        String sessionTitle = exam.getSession() == null ? null : exam.getSession().getTitle();
        java.time.LocalDate sessionDate = exam.getSession() == null ? null : exam.getSession().getSessionDate();
        // §Phan C: breakdown cua bai thi CHI tinh tren cau hoi cua CHINH bai
        // thi nay (khong gop cac de khac cung chuong) — doi tu loc topicId
        // sang loc examId de "vao bai thi X" khong lan du lieu bai thi Y.
        BreakdownResponse breakdown = breakdowns(studentId, classId, null, null, examId);

        // PHO DIEM cua bai thi (§12) — 1 call gop san (nhieu khoang cho kieu pho diem).
        ExamScoreDistribution distribution =
                buildDistribution(examId, exam.getName(), classId, studentId, 40);

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
                sessionId,
                sessionTitle,
                sessionDate,
                breakdown,
                distribution);
    }

    public List<ScoreTrendPoint> scoreTrend(Long studentId, Long classId, Long topicId) {
        return aggregationRepository.trendForStudentInClass(studentId, classId, topicId).stream()
                .map(ReportMapper::trendPoint)
                .toList();
    }

    /**
     * Breakdown NANG LUC theo chuong (topicId nullable = toan khoa), theo
     * buoi hoc (sessionId nullable), hoac theo 1 bai thi cu the (examId
     * nullable). §11 + bao cao cap buoi/bai thi. 3 filter doc lap — FE chi
     * truyen dung 1 loai tuy man hinh.
     */
    public BreakdownResponse breakdowns(Long studentId, Long classId, Long topicId, Long sessionId,
                                        Long examId) {
        return ReportMapper.breakdown(
                aggregationRepository.difficultyBreakdown(studentId, classId, topicId, sessionId, examId),
                aggregationRepository.typeBreakdown(studentId, classId, topicId, sessionId, examId));
    }

    /** Bao cao cap buoi hoc (§ Bao cao 3 cap do): de thi + breakdown cua 1 buoi cu the. */
    public List<RecentExamItem> sessionExams(Long studentId, Long classId, Long sessionId) {
        requireStudentInClass(studentId, classId);
        requireSessionInClass(sessionId, classId);
        return aggregationRepository.sessionExamsForStudent(studentId, classId, sessionId).stream()
                .map(ReportMapper::recentExam)
                .toList();
    }

    public BreakdownResponse sessionBreakdowns(Long studentId, Long classId, Long sessionId) {
        requireStudentInClass(studentId, classId);
        requireSessionInClass(sessionId, classId);
        return breakdowns(studentId, classId, null, sessionId, null);
    }

    private void requireSessionInClass(Long sessionId, Long classId) {
        if (!classSessionRepository.existsByIdAndClazzId(sessionId, classId)) {
            throw new AppException(ErrorCode.SESSION_NOT_IN_CLASS);
        }
    }

    /** Pho diem 1 bai thi cho 1 HV (§12.1) — co danh dau vi tri HV. bandCount tuy chon. */
    public ExamScoreDistribution scoreDistribution(Long studentId, Long examId, Long classId,
                                                   int bands) {
        requireStudentInClass(studentId, classId);
        ExamStudent es = examStudentRepository.findByExamIdAndUserId(examId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_AVAILABLE));
        boolean examInClass = es.getExam().getClasses().stream()
                .anyMatch(c -> c.getId().equals(classId));
        if (!examInClass) {
            throw new AppException(ErrorCode.EXAM_NOT_IN_CLASS);
        }
        return buildDistribution(examId, es.getExam().getName(), classId, studentId, bands);
    }

    /** Pho diem TONG cua khoa (§12.2) — bucket diem TB HV, danh dau HV neu co studentId. */
    public ExamScoreDistribution courseSpectrum(Long studentId, Long classId, int bandCount) {
        if (studentId != null) {
            requireStudentInClass(studentId, classId);
        }
        var rows = aggregationRepository.studentAveragesForClass(classId);
        List<Double> values = new java.util.ArrayList<>();
        Double studentValue = null;
        for (var r : rows) {
            if (r.getAvgPct() == null) {
                continue;
            }
            double v = r.getAvgPct() * 10.0;
            values.add(v);
            if (studentId != null && studentId.equals(r.getStudentId())) {
                studentValue = v;
            }
        }
        int classSize = (int) schoolClassRepository.countStudents(classId);
        return ReportMapper.spectrum("Điểm trung bình khóa", values, studentValue,
                Math.max(5, Math.min(bandCount, 60)), classSize);
    }

    private ExamScoreDistribution buildDistribution(Long examId, String examName,
                                                    Long classId, Long studentId, int nBands) {
        int n = Math.max(5, Math.min(nBands, 60));
        BigDecimal maxScore = aggregationRepository.examMaxScore(examId);
        int classSize = (int) schoolClassRepository.countStudents(classId);
        Integer rank = null;
        if (studentId != null) {
            RankStatsProjection rs = aggregationRepository.rankAndStats(examId, classId, studentId);
            rank = rs == null || rs.getRank() == null ? null : rs.getRank().intValue();
        }
        List<ScoreBandProjection> bands = maxScore != null && maxScore.signum() > 0
                ? aggregationRepository.scoreDistribution(examId, classId, maxScore, n)
                : List.of();
        ScoreStatsProjection stats = aggregationRepository.scoreStats(examId, classId, studentId);
        return ReportMapper.distribution(examId, examName, maxScore, n, bands,
                stats, classSize, rank);
    }

    public List<TopicMasteryItem> topicMastery(Long studentId, Long classId) {
        return aggregationRepository.topicMastery(studentId, classId).stream()
                .map(ReportMapper::topic)
                .toList();
    }

    public StudentAttendanceReport attendance(Long studentId, Long classId, Long topicId) {
        return new StudentAttendanceReport(
                ReportMapper.attendance(aggregationRepository.attendanceForStudent(studentId, classId, topicId)),
                aggregationRepository.attendanceByMonthForStudent(studentId, classId, topicId).stream()
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
                scoreTrend(studentId, classId, null),
                breakdowns(studentId, classId, null, null, null),
                topicMastery(studentId, classId),
                attendance(studentId, classId, null),
                reportCommentService.list(studentId, classId, null),
                analysis(studentId, classId));
    }

    /**
     * Bang "Phan tich tu dong" o dau bao cao ca nhan: cau chu tieng Viet da
     * ghep san (§ Analysis Card). Goi lai cac ham/query da co, chi them
     * xep hang theo diem TB (chua ton tai — rankAndStats chi xep theo 1 bai).
     */
    public ReportAnalysisResponse analysis(Long studentId, Long classId) {
        requireStudentInClass(studentId, classId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        SchoolClass clazz = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        int classSize = (int) schoolClassRepository.countStudents(classId);

        // ---- pho diem + xep hang toan khoa ----
        List<StudentAvgProjection> allAverages = aggregationRepository.studentAveragesForClass(classId);
        String spectrumLabel = narrativeBuilder.scoreSpectrumLabel(allAverages);

        Map<Long, BigDecimal> courseScores = allAverages.stream()
                .filter(p -> p.getAvgScore() != null)
                .collect(Collectors.toMap(StudentAvgProjection::getStudentId, StudentAvgProjection::getAvgScore,
                        (a, b) -> a, LinkedHashMap::new));
        Map<Long, Integer> courseRanks = narrativeBuilder.denseRankDesc(courseScores);
        BigDecimal courseAverage = courseScores.get(studentId);
        Integer courseRank = courseRanks.get(studentId);

        // ---- xep hang tung chuong (topic cua DE THI) ----
        List<StudentTopicAvgProjection> topicRows = aggregationRepository.studentTopicAverages(classId);
        List<ChapterAnalysisItem> chapters = buildChapters(studentId, topicRows);

        // ---- nhan dinh nang luc (loai cau x do kho) ----
        List<String> abilityInsights = narrativeBuilder.abilityInsights(
                aggregationRepository.breakdownByTypeAndDifficulty(studentId, classId, null, null, null));

        // ---- chuyen can ----
        String attendanceInsight = narrativeBuilder.attendanceInsight(
                attendance(studentId, classId, null).summary());

        // ---- nhan xet GV moi nhat ----
        CommentItem teacherComment = reportCommentService.list(studentId, classId, null).stream()
                .filter(c -> "TEACHER".equals(c.authorRole()))
                .findFirst()
                .orElse(null);

        return new ReportAnalysisResponse(
                student.getFullName(),
                clazz.getName(),
                spectrumLabel,
                courseAverage,
                courseRank,
                classSize,
                chapters,
                abilityInsights,
                attendanceInsight,
                teacherComment == null ? null : teacherComment.authorName(),
                teacherComment == null ? null : teacherComment.content());
    }

    /**
     * Chi liet ke chuong HV DA CO diem (co mat trong studentTopicAverages) —
     * KHONG dung topicMastery.gradedCount de loc: topicMastery nhom theo
     * topic cua CAU HOI, khac topic cua DE THI dung o day, 2 bo co the lech.
     */
    private List<ChapterAnalysisItem> buildChapters(Long studentId, List<StudentTopicAvgProjection> topicRows) {
        record TopicMeta(String name, Integer sortOrder) {}
        Map<Long, TopicMeta> metaByTopic = new LinkedHashMap<>();
        Map<Long, List<StudentTopicAvgProjection>> rowsByTopic = new LinkedHashMap<>();
        for (StudentTopicAvgProjection r : topicRows) {
            metaByTopic.putIfAbsent(r.getTopicId(), new TopicMeta(r.getTopicName(), r.getSortOrder()));
            rowsByTopic.computeIfAbsent(r.getTopicId(), k -> new ArrayList<>()).add(r);
        }

        List<Long> orderedTopicIds = metaByTopic.keySet().stream()
                .sorted(Comparator.<Long, Integer>comparing(
                                id -> metaByTopic.get(id).sortOrder(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(id -> id))
                .toList();

        List<ChapterAnalysisItem> chapters = new ArrayList<>();
        int index = 0;
        for (Long topicId : orderedTopicIds) {
            List<StudentTopicAvgProjection> rows = rowsByTopic.get(topicId);
            Map<Long, BigDecimal> scores = rows.stream()
                    .filter(r -> r.getAvgScore() != null)
                    .collect(Collectors.toMap(StudentTopicAvgProjection::getStudentId,
                            StudentTopicAvgProjection::getAvgScore, (a, b) -> a, LinkedHashMap::new));
            BigDecimal myScore = scores.get(studentId);
            if (myScore == null) {
                continue; // HV chua co diem chuong nay -> khong liet ke
            }
            index++;
            Map<Long, Integer> ranks = narrativeBuilder.denseRankDesc(scores);
            chapters.add(new ChapterAnalysisItem(
                    topicId, narrativeBuilder.chapterLabel(index), myScore,
                    ranks.get(studentId), scores.size()));
        }
        return chapters;
    }

    /** Bang "Phan tich tu dong" cho 1 CHUONG (§ Phan C) — tai dung buildChapters(). */
    public ChapterAnalysisResponse chapterAnalysis(Long studentId, Long classId, Long topicId) {
        requireStudentInClass(studentId, classId);
        List<StudentTopicAvgProjection> topicRows = aggregationRepository.studentTopicAverages(classId);
        ChapterAnalysisItem item = buildChapters(studentId, topicRows).stream()
                .filter(c -> topicId.equals(c.topicId()))
                .findFirst()
                .orElse(new ChapterAnalysisItem(topicId, null, null, null, null));

        List<String> abilityInsights = narrativeBuilder.abilityInsights(
                aggregationRepository.breakdownByTypeAndDifficulty(studentId, classId, topicId, null, null));
        String attendanceInsight = narrativeBuilder.attendanceInsight(
                attendance(studentId, classId, topicId).summary());

        return new ChapterAnalysisResponse(
                item.chapterLabel(), item.avgScore(), item.rank(), item.classSize(),
                abilityInsights, attendanceInsight);
    }

    /** Bang "Phan tich tu dong" cho 1 BUOI HOC (§ Phan C). */
    public SessionAnalysisResponse sessionAnalysis(Long studentId, Long classId, Long sessionId) {
        requireStudentInClass(studentId, classId);
        requireSessionInClass(sessionId, classId);

        List<BigDecimal> myScores = aggregationRepository.sessionExamsForStudent(studentId, classId, sessionId)
                .stream().map(ReportAggregationRepository.RecentExamProjection::getScore)
                .filter(s -> s != null)
                .toList();
        BigDecimal avgScore = average(myScores);

        List<ClassExamAvgProjection> classExamAverages =
                aggregationRepository.sessionExamAveragesForClass(classId, sessionId);
        List<BigDecimal> classAvgScores = classExamAverages.stream()
                .map(ClassExamAvgProjection::getAvgScore)
                .filter(s -> s != null)
                .toList();
        BigDecimal classAverage = average(classAvgScores);

        String comparisonInsight = narrativeBuilder.comparisonInsight(avgScore, classAverage);
        List<String> abilityInsights = narrativeBuilder.abilityInsights(
                aggregationRepository.breakdownByTypeAndDifficulty(studentId, classId, null, sessionId, null));

        return new SessionAnalysisResponse(
                avgScore, classAverage, comparisonInsight, abilityInsights,
                classExamAverages.size(), myScores.size());
    }

    /** Bang "Phan tich tu dong" cho 1 BAI THI (§ Phan C) — tai dung rankAndStats() nhu examDetail(). */
    public ExamAnalysisResponse examAnalysis(Long studentId, Long examId, Long classId) {
        requireStudentInClass(studentId, classId);
        ExamStudent es = examStudentRepository.findByExamIdAndUserId(examId, studentId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_AVAILABLE));
        boolean examInClass = es.getExam().getClasses().stream()
                .anyMatch(c -> c.getId().equals(classId));
        if (!examInClass) {
            throw new AppException(ErrorCode.EXAM_NOT_IN_CLASS);
        }

        RankStatsProjection stats = aggregationRepository.rankAndStats(examId, classId, studentId);
        int classSize = (int) schoolClassRepository.countStudents(classId);
        BigDecimal classAverage = stats == null ? null : ReportMapper.scale(stats.getClassAverage());
        String comparisonInsight = narrativeBuilder.comparisonInsight(es.getScore(), classAverage);
        List<String> abilityInsights = narrativeBuilder.abilityInsights(
                aggregationRepository.breakdownByTypeAndDifficulty(studentId, classId, null, null, examId));

        return new ExamAnalysisResponse(
                es.getScore(), classAverage,
                stats == null || stats.getRank() == null ? null : stats.getRank().intValue(),
                classSize, comparisonInsight, abilityInsights);
    }

    private static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
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
