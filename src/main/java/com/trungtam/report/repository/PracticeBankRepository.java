package com.trungtam.report.repository;

import com.trungtam.exercise.entity.Exercise;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Truy van kho bai tap phuc vu sinh de luyen tap (§10). Loai ESSAY (G1).
 */
public interface PracticeBankRepository extends Repository<Exercise, Long> {

    /** Bai ACTIVE cua 1 chuyen de (subject+topic), tru tu luan. */
    @Query(value = """
            SELECT ex.id AS id, ex.difficulty AS difficulty, ex.type AS type
            FROM exercises ex
            WHERE ex.subject_id = :subjectId AND ex.topic_id = :topicId
              AND ex.status = 'ACTIVE' AND ex.type <> 'ESSAY'
            """, nativeQuery = true)
    List<BankExercise> bankByTopic(@Param("subjectId") Long subjectId,
                                   @Param("topicId") Long topicId);

    /** Cac bai HV da gap (o bat ky de nao co dong exam_student). */
    @Query(value = """
            SELECT DISTINCT ee.exercise_id
            FROM exam_exercises ee
            JOIN exam_student es ON es.exam_id = ee.exam_id AND es.user_id = :studentId
            """, nativeQuery = true)
    List<Long> seenExerciseIds(@Param("studentId") Long studentId);

    /** Suy chuyen de tu bai thi: chuyen de co nhieu cau HV lam SAI nhat. */
    @Query(value = """
            SELECT ex.topic_id
            FROM exam_question_result r
            JOIN exam_student es ON es.id = r.exam_student_id AND es.user_id = :studentId AND es.exam_id = :examId
            JOIN exam_exercises ee ON ee.id = r.exam_exercise_id
            JOIN exercises ex ON ex.id = ee.exercise_id
            WHERE ex.topic_id IS NOT NULL
            GROUP BY ex.topic_id
            ORDER BY COUNT(*) FILTER (WHERE r.correct IS FALSE) DESC, COUNT(*) DESC
            LIMIT 1
            """, nativeQuery = true)
    Long inferTopicFromExam(@Param("studentId") Long studentId, @Param("examId") Long examId);

    interface BankExercise {
        Long getId();
        String getDifficulty();
        String getType();
    }
}
