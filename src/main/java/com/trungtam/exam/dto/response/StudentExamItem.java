package com.trungtam.exam.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 1 dong de thi trong popup "De thi" cua hoc vien (theo khoa).
 * status la trang thai HIEU LUC (da overlay QUA_HAN); canView/canTake
 * cho FE biet nut nao duoc bat (theo dung dac ta phia hoc vien).
 */
public record StudentExamItem(
        Long examId,
        String code,
        String name,
        List<CourseRef> courses,
        Instant publishAt,
        Instant endAt,
        Integer durationMinutes,
        String status,
        boolean canView,
        boolean canTake
) {
    public record CourseRef(Long id, String name) {}
}
