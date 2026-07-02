package com.trungtam.report.repository;

import com.trungtam.exam.entity.ExamStudent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Tong hop du lieu bao cao bang native query (DENSE_RANK, FILTER, GROUP BY).
 * Map ket qua bang interface projection — alias cot khop ten getter (case-insensitive).
 * Moi query chi tinh bai DA_LAM. Cot timestamptz duoc Hibernate tra ve java.time.Instant.
 */
public interface ReportAggregationRepository extends Repository<ExamStudent, Long> {

    // ============ BAI DA NOP ============

    @Query(value = """
            SELECT es.id AS examStudentId, e.id AS examId, e.code AS examCode, e.name AS examName,
                   s.name AS subjectName, cc.class_id AS classId, cc.class_name AS className,
                   es.submitted_at AS submittedAt, es.score AS score,
                   (SELECT COALESCE(SUM(r.max_points),0) FROM exam_question_result r
                     WHERE r.exam_student_id = es.id) AS maxScore
            FROM exam_student es
            JOIN exams e    ON e.id = es.exam_id
            JOIN subjects s ON s.id = e.subject_id
            LEFT JOIN LATERAL (
                SELECT c.id AS class_id, c.name AS class_name
                FROM exam_classes ec JOIN classes c ON c.id = ec.class_id
                JOIN class_students cs ON cs.class_id = c.id AND cs.user_id = es.user_id
                WHERE ec.exam_id = e.id ORDER BY c.id LIMIT 1) cc ON TRUE
            WHERE es.user_id = :studentId AND es.status = 'DA_LAM'
            ORDER BY es.submitted_at DESC NULLS LAST
            LIMIT :limit
            """, nativeQuery = true)
    List<RecentExamProjection> recentSubmitted(@Param("studentId") Long studentId,
                                               @Param("limit") int limit);

    @Query(value = """
            SELECT es.id AS examStudentId, e.id AS examId, e.code AS examCode, e.name AS examName,
                   s.name AS subjectName, c.id AS classId, c.name AS className,
                   es.submitted_at AS submittedAt, es.score AS score,
                   (SELECT COALESCE(SUM(r.max_points),0) FROM exam_question_result r
                     WHERE r.exam_student_id = es.id) AS maxScore
            FROM exam_student es
            JOIN exams e    ON e.id = es.exam_id
            JOIN subjects s ON s.id = e.subject_id
            JOIN classes c  ON c.id = :classId
            WHERE es.user_id = :studentId AND es.status = 'DA_LAM'
              AND EXISTS (SELECT 1 FROM exam_classes ec WHERE ec.exam_id = e.id AND ec.class_id = :classId)
            ORDER BY es.submitted_at DESC NULLS LAST
            """, nativeQuery = true)
    List<RecentExamProjection> historyInClass(@Param("studentId") Long studentId,
                                              @Param("classId") Long classId);

    // ============ HANG + TB LOP (1 de) ============

    @Query(value = """
            WITH pop AS (
                SELECT es.user_id, es.score,
                       DENSE_RANK() OVER (ORDER BY es.score DESC) AS rnk
                FROM exam_student es
                JOIN class_students cs ON cs.user_id = es.user_id AND cs.class_id = :classId
                WHERE es.exam_id = :examId AND es.status = 'DA_LAM')
            SELECT (SELECT rnk FROM pop WHERE user_id = :studentId) AS rank,
                   AVG(score) AS classAverage,
                   COUNT(*) AS submittedCount
            FROM pop
            """, nativeQuery = true)
    RankStatsProjection rankAndStats(@Param("examId") Long examId,
                                     @Param("classId") Long classId,
                                     @Param("studentId") Long studentId);

    // ============ TB LOP QUA CAC DE / XU HUONG ============

    @Query(value = """
            SELECT e.id AS examId, e.name AS examName, e.publish_at AS publishAt,
                   AVG(es.score) AS avgScore,
                   (SELECT COALESCE(SUM(CASE
                          WHEN EXISTS (SELECT 1 FROM exam_tf_item_scores ts WHERE ts.exam_exercise_id = ee.id)
                          THEN (SELECT COALESCE(SUM(ts.points),0) FROM exam_tf_item_scores ts WHERE ts.exam_exercise_id = ee.id)
                          ELSE COALESCE(ee.points,0) END),0)
                    FROM exam_exercises ee WHERE ee.exam_id = e.id) AS maxScore,
                   COUNT(es.id) AS submittedCount,
                   COUNT(DISTINCT cs.user_id) AS assignedCount
            FROM exams e
            JOIN exam_classes ec   ON ec.exam_id = e.id AND ec.class_id = :classId
            JOIN class_students cs ON cs.class_id = :classId
            LEFT JOIN exam_student es ON es.exam_id = e.id AND es.user_id = cs.user_id AND es.status = 'DA_LAM'
            GROUP BY e.id, e.name, e.publish_at
            ORDER BY e.publish_at NULLS LAST, e.id
            """, nativeQuery = true)
    List<ClassExamAvgProjection> examAveragesForClass(@Param("classId") Long classId);

    @Query(value = """
            SELECT e.id AS examId, e.name AS examName, e.publish_at AS publishAt,
                   es.submitted_at AS submittedAt, es.score AS score,
                   (SELECT COALESCE(SUM(r.max_points),0) FROM exam_question_result r
                     WHERE r.exam_student_id = es.id) AS maxScore,
                   (SELECT AVG(es2.score) FROM exam_student es2
                      JOIN class_students cs2 ON cs2.user_id = es2.user_id AND cs2.class_id = :classId
                      WHERE es2.exam_id = e.id AND es2.status = 'DA_LAM') AS classAverage
            FROM exam_student es
            JOIN exams e         ON e.id = es.exam_id
            JOIN exam_classes ec ON ec.exam_id = e.id AND ec.class_id = :classId
            WHERE es.user_id = :studentId AND es.status = 'DA_LAM'
            ORDER BY e.publish_at NULLS LAST, es.submitted_at
            """, nativeQuery = true)
    List<ScoreTrendProjection> trendForStudentInClass(@Param("studentId") Long studentId,
                                                      @Param("classId") Long classId);

    // ============ BREAKDOWN DUNG/SAI ============

    // Breakdown theo CHUONG (topicId nullable = toan khoa). §11: nang luc tinh tren
    // MOI cau thuoc chuong tu MOI bai DA_LAM cua HV trong lop (khong theo 1 bai thi).
    @Query(value = """
            SELECT ex.difficulty AS bucketKey,
                   COUNT(*) FILTER (WHERE r.correct IS TRUE)  AS correctCount,
                   COUNT(*) FILTER (WHERE r.correct IS FALSE) AS incorrectCount,
                   COUNT(*) FILTER (WHERE r.correct IS NULL)  AS ungradedCount
            FROM exam_question_result r
            JOIN exam_student es   ON es.id = r.exam_student_id AND es.status = 'DA_LAM'
            JOIN exam_classes ec   ON ec.exam_id = es.exam_id AND ec.class_id = :classId
            JOIN class_students cs ON cs.class_id = :classId AND cs.user_id = es.user_id
            JOIN exam_exercises ee ON ee.id = r.exam_exercise_id
            JOIN exercises ex      ON ex.id = ee.exercise_id
            WHERE (CAST(:studentId AS BIGINT) IS NULL OR es.user_id = :studentId)
              AND (CAST(:topicId AS BIGINT) IS NULL OR ex.topic_id = :topicId)
            GROUP BY ex.difficulty
            """, nativeQuery = true)
    List<BreakdownProjection> difficultyBreakdown(@Param("studentId") Long studentId,
                                                  @Param("classId") Long classId,
                                                  @Param("topicId") Long topicId);

    @Query(value = """
            SELECT ex.type AS bucketKey,
                   COUNT(*) FILTER (WHERE r.correct IS TRUE)  AS correctCount,
                   COUNT(*) FILTER (WHERE r.correct IS FALSE) AS incorrectCount,
                   COUNT(*) FILTER (WHERE r.correct IS NULL)  AS ungradedCount
            FROM exam_question_result r
            JOIN exam_student es   ON es.id = r.exam_student_id AND es.status = 'DA_LAM'
            JOIN exam_classes ec   ON ec.exam_id = es.exam_id AND ec.class_id = :classId
            JOIN class_students cs ON cs.class_id = :classId AND cs.user_id = es.user_id
            JOIN exam_exercises ee ON ee.id = r.exam_exercise_id
            JOIN exercises ex      ON ex.id = ee.exercise_id
            WHERE (CAST(:studentId AS BIGINT) IS NULL OR es.user_id = :studentId)
              AND (CAST(:topicId AS BIGINT) IS NULL OR ex.topic_id = :topicId)
            GROUP BY ex.type
            """, nativeQuery = true)
    List<BreakdownProjection> typeBreakdown(@Param("studentId") Long studentId,
                                            @Param("classId") Long classId,
                                            @Param("topicId") Long topicId);

    // ============ PHO DIEM (§12) ============

    /** Diem toi da cua 1 de = tong diem tung cau (TF co bang diem tung y -> tong y). */
    @Query(value = """
            SELECT COALESCE(SUM(CASE
                   WHEN EXISTS (SELECT 1 FROM exam_tf_item_scores ts WHERE ts.exam_exercise_id = ee.id)
                   THEN (SELECT COALESCE(SUM(ts.points),0) FROM exam_tf_item_scores ts WHERE ts.exam_exercise_id = ee.id)
                   ELSE COALESCE(ee.points,0) END),0)
            FROM exam_exercises ee WHERE ee.exam_id = :examId
            """, nativeQuery = true)
    java.math.BigDecimal examMaxScore(@Param("examId") Long examId);

    /** Histogram: chia [0..maxScore] thanh :bandCount khoang deu; dem HV DA_LAM moi khoang. */
    @Query(value = """
            SELECT b.idx AS index, COALESCE(cnt.c, 0) AS count
            FROM generate_series(1, :bandCount) AS b(idx)
            LEFT JOIN (
                SELECT LEAST(width_bucket(es.score, 0, :maxScore, :bandCount), :bandCount) AS bkt,
                       COUNT(*) AS c
                FROM exam_student es
                JOIN class_students cs ON cs.user_id = es.user_id AND cs.class_id = :classId
                WHERE es.exam_id = :examId AND es.status = 'DA_LAM' AND es.score IS NOT NULL
                GROUP BY 1
            ) cnt ON cnt.bkt = b.idx
            ORDER BY b.idx
            """, nativeQuery = true)
    List<ScoreBandProjection> scoreDistribution(@Param("examId") Long examId,
                                                @Param("classId") Long classId,
                                                @Param("maxScore") java.math.BigDecimal maxScore,
                                                @Param("bandCount") int bandCount);

    /** Thong ke pho diem + vi tri HV (studentId nullable -> studentScore/leCount = null). */
    @Query(value = """
            SELECT AVG(es.score) AS avgScore,
                   percentile_cont(0.5) WITHIN GROUP (ORDER BY es.score) AS median,
                   MAX(es.score) AS highest, MIN(es.score) AS lowest,
                   COUNT(*) AS submittedCount,
                   (SELECT es2.score FROM exam_student es2
                      JOIN class_students cs2 ON cs2.user_id = es2.user_id AND cs2.class_id = :classId
                      WHERE es2.exam_id = :examId AND es2.status = 'DA_LAM'
                        AND es2.user_id = :studentId) AS studentScore,
                   COUNT(*) FILTER (WHERE CAST(:studentId AS BIGINT) IS NOT NULL AND es.score <=
                      (SELECT es3.score FROM exam_student es3
                         JOIN class_students cs3 ON cs3.user_id = es3.user_id AND cs3.class_id = :classId
                         WHERE es3.exam_id = :examId AND es3.status = 'DA_LAM'
                           AND es3.user_id = :studentId)) AS leCount
            FROM exam_student es
            JOIN class_students cs ON cs.user_id = es.user_id AND cs.class_id = :classId
            WHERE es.exam_id = :examId AND es.status = 'DA_LAM' AND es.score IS NOT NULL
            """, nativeQuery = true)
    ScoreStatsProjection scoreStats(@Param("examId") Long examId,
                                    @Param("classId") Long classId,
                                    @Param("studentId") Long studentId);

    // ============ NAM CHAC THEO CHUONG ============

    @Query(value = """
            SELECT t.id AS topicId, t.name AS topicName,
                   COUNT(*) FILTER (WHERE r.correct IS NOT NULL) AS gradedCount,
                   COUNT(*) FILTER (WHERE r.correct IS TRUE)     AS correctCount,
                   COUNT(*) FILTER (WHERE r.correct IS NULL)     AS ungradedCount,
                   COALESCE(SUM(r.earned)     FILTER (WHERE r.correct IS NOT NULL),0) AS earned,
                   COALESCE(SUM(r.max_points) FILTER (WHERE r.correct IS NOT NULL),0) AS max
            FROM exam_question_result r
            JOIN exam_student es   ON es.id = r.exam_student_id AND es.status = 'DA_LAM'
            JOIN exam_classes ec   ON ec.exam_id = es.exam_id AND ec.class_id = :classId
            JOIN class_students cs ON cs.class_id = :classId AND cs.user_id = es.user_id
            JOIN exam_exercises ee ON ee.id = r.exam_exercise_id
            JOIN exercises ex      ON ex.id = ee.exercise_id
            LEFT JOIN topics t     ON t.id = ex.topic_id
            WHERE (CAST(:studentId AS BIGINT) IS NULL OR es.user_id = :studentId)
            GROUP BY t.id, t.name, t.sort_order
            ORDER BY t.sort_order NULLS LAST, t.name NULLS LAST
            """, nativeQuery = true)
    List<TopicMasteryProjection> topicMastery(@Param("studentId") Long studentId,
                                              @Param("classId") Long classId);

    // ============ DIEM TB TUNG HV CUA LOP ============

    @Query(value = """
            SELECT u.id AS studentId, u.full_name AS fullName, u.username AS username,
                   COUNT(es.id) AS submittedCount,
                   AVG(es.score) AS avgScore,
                   AVG(CASE WHEN mx.max_score > 0 THEN es.score / mx.max_score END) AS avgPct
            FROM class_students cs
            JOIN users u ON u.id = cs.user_id
            LEFT JOIN exam_student es ON es.user_id = cs.user_id AND es.status = 'DA_LAM'
                 AND EXISTS (SELECT 1 FROM exam_classes ec WHERE ec.exam_id = es.exam_id AND ec.class_id = :classId)
            LEFT JOIN LATERAL (
                SELECT COALESCE(SUM(CASE
                       WHEN EXISTS (SELECT 1 FROM exam_tf_item_scores ts WHERE ts.exam_exercise_id = ee.id)
                       THEN (SELECT COALESCE(SUM(ts.points),0) FROM exam_tf_item_scores ts WHERE ts.exam_exercise_id = ee.id)
                       ELSE COALESCE(ee.points,0) END),0) AS max_score
                FROM exam_exercises ee WHERE ee.exam_id = es.exam_id) mx ON TRUE
            WHERE cs.class_id = :classId
            GROUP BY u.id, u.full_name, u.username
            ORDER BY u.full_name
            """, nativeQuery = true)
    List<StudentAvgProjection> studentAveragesForClass(@Param("classId") Long classId);

    // ============ CHUYEN CAN ============

    @Query(value = """
            SELECT COUNT(*) AS totalSessions,
                   COUNT(*) FILTER (WHERE sa.status = 'CO_MAT')       AS coMat,
                   COUNT(*) FILTER (WHERE sa.status = 'TRE')          AS tre,
                   COUNT(*) FILTER (WHERE sa.status = 'VANG')         AS vang,
                   COUNT(*) FILTER (WHERE sa.status = 'CO_PHEP')      AS coPhep,
                   COUNT(*) FILTER (WHERE sa.status = 'CHUA_CHECKIN') AS chuaCheckin
            FROM session_attendance sa
            JOIN class_sessions ses ON ses.id = sa.session_id
                 AND ses.class_id = :classId AND ses.status = 'DONE'
            WHERE sa.user_id = :studentId
            """, nativeQuery = true)
    AttendanceProjection attendanceForStudent(@Param("studentId") Long studentId,
                                              @Param("classId") Long classId);

    @Query(value = """
            SELECT to_char(ses.session_date, 'YYYY-MM') AS month,
                   COUNT(*) FILTER (WHERE sa.status = 'CO_MAT')  AS coMat,
                   COUNT(*) FILTER (WHERE sa.status = 'TRE')     AS tre,
                   COUNT(*) FILTER (WHERE sa.status = 'VANG')    AS vang,
                   COUNT(*) FILTER (WHERE sa.status = 'CO_PHEP') AS coPhep
            FROM session_attendance sa
            JOIN class_sessions ses ON ses.id = sa.session_id
                 AND ses.class_id = :classId AND ses.status = 'DONE'
            WHERE sa.user_id = :studentId
            GROUP BY to_char(ses.session_date, 'YYYY-MM')
            ORDER BY 1
            """, nativeQuery = true)
    List<AttendanceMonthProjection> attendanceByMonthForStudent(@Param("studentId") Long studentId,
                                                                @Param("classId") Long classId);

    @Query(value = """
            SELECT COUNT(*) AS totalSessions,
                   COUNT(*) FILTER (WHERE sa.status = 'CO_MAT')       AS coMat,
                   COUNT(*) FILTER (WHERE sa.status = 'TRE')          AS tre,
                   COUNT(*) FILTER (WHERE sa.status = 'VANG')         AS vang,
                   COUNT(*) FILTER (WHERE sa.status = 'CO_PHEP')      AS coPhep,
                   COUNT(*) FILTER (WHERE sa.status = 'CHUA_CHECKIN') AS chuaCheckin
            FROM session_attendance sa
            JOIN class_sessions ses ON ses.id = sa.session_id
                 AND ses.class_id = :classId AND ses.status = 'DONE'
            JOIN class_students cs ON cs.class_id = :classId AND cs.user_id = sa.user_id
            """, nativeQuery = true)
    AttendanceProjection attendanceForClass(@Param("classId") Long classId);

    @Query(value = """
            SELECT to_char(ses.session_date, 'YYYY-MM') AS month,
                   COUNT(*) FILTER (WHERE sa.status = 'CO_MAT')  AS coMat,
                   COUNT(*) FILTER (WHERE sa.status = 'TRE')     AS tre,
                   COUNT(*) FILTER (WHERE sa.status = 'VANG')    AS vang,
                   COUNT(*) FILTER (WHERE sa.status = 'CO_PHEP') AS coPhep
            FROM session_attendance sa
            JOIN class_sessions ses ON ses.id = sa.session_id
                 AND ses.class_id = :classId AND ses.status = 'DONE'
            JOIN class_students cs ON cs.class_id = :classId AND cs.user_id = sa.user_id
            GROUP BY to_char(ses.session_date, 'YYYY-MM')
            ORDER BY 1
            """, nativeQuery = true)
    List<AttendanceMonthProjection> attendanceByMonthForClass(@Param("classId") Long classId);

    @Query(value = """
            SELECT sa.user_id AS studentId, COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE sa.status = 'CO_MAT') AS coMat,
                   COUNT(*) FILTER (WHERE sa.status = 'TRE')    AS tre
            FROM session_attendance sa
            JOIN class_sessions ses ON ses.id = sa.session_id
                 AND ses.class_id = :classId AND ses.status = 'DONE'
            JOIN class_students cs ON cs.class_id = :classId AND cs.user_id = sa.user_id
            GROUP BY sa.user_id
            """, nativeQuery = true)
    List<AttendanceRateProjection> attendanceRatePerStudent(@Param("classId") Long classId);

    // ============ PROJECTIONS ============

    interface RecentExamProjection {
        Long getExamStudentId();
        Long getExamId();
        String getExamCode();
        String getExamName();
        String getSubjectName();
        Long getClassId();
        String getClassName();
        Instant getSubmittedAt();
        BigDecimal getScore();
        BigDecimal getMaxScore();
    }

    interface RankStatsProjection {
        Long getRank();
        BigDecimal getClassAverage();
        Long getSubmittedCount();
    }

    interface ClassExamAvgProjection {
        Long getExamId();
        String getExamName();
        Instant getPublishAt();
        BigDecimal getAvgScore();
        BigDecimal getMaxScore();
        Long getSubmittedCount();
        Long getAssignedCount();
    }

    interface ScoreTrendProjection {
        Long getExamId();
        String getExamName();
        Instant getPublishAt();
        Instant getSubmittedAt();
        BigDecimal getScore();
        BigDecimal getMaxScore();
        BigDecimal getClassAverage();
    }

    interface BreakdownProjection {
        String getBucketKey();
        Long getCorrectCount();
        Long getIncorrectCount();
        Long getUngradedCount();
    }

    interface TopicMasteryProjection {
        Long getTopicId();
        String getTopicName();
        Long getGradedCount();
        Long getCorrectCount();
        Long getUngradedCount();
        BigDecimal getEarned();
        BigDecimal getMax();
    }

    interface StudentAvgProjection {
        Long getStudentId();
        String getFullName();
        String getUsername();
        Long getSubmittedCount();
        BigDecimal getAvgScore();
        Double getAvgPct();
    }

    interface AttendanceProjection {
        Long getTotalSessions();
        Long getCoMat();
        Long getTre();
        Long getVang();
        Long getCoPhep();
        Long getChuaCheckin();
    }

    interface AttendanceMonthProjection {
        String getMonth();
        Long getCoMat();
        Long getTre();
        Long getVang();
        Long getCoPhep();
    }

    interface AttendanceRateProjection {
        Long getStudentId();
        Long getTotal();
        Long getCoMat();
        Long getTre();
    }

    interface ScoreBandProjection {
        Integer getIndex();
        Long getCount();
    }

    interface ScoreStatsProjection {
        BigDecimal getAvgScore();
        Double getMedian();
        BigDecimal getHighest();
        BigDecimal getLowest();
        Long getSubmittedCount();
        BigDecimal getStudentScore();
        Long getLeCount();
    }
}
