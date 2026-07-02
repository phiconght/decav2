package com.trungtam.exam.dto.request;

import java.util.Map;

/**
 * Cau tra loi cua hoc vien, key theo examExerciseId.
 * Dung chung cho: nop bai, luu nhap, va serialize vao cot exam_student.answers.
 */
public record SubmitExamRequest(
        Map<Long, Long> mc,
        Map<Long, Map<Long, Boolean>> tf,
        Map<Long, String> essay
) {
    public SubmitExamRequest {
        mc = mc == null ? Map.of() : mc;
        tf = tf == null ? Map.of() : tf;
        essay = essay == null ? Map.of() : essay;
    }

    public static SubmitExamRequest empty() {
        return new SubmitExamRequest(null, null, null);
    }
}
