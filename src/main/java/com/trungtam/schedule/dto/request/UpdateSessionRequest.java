package com.trungtam.schedule.dto.request;

import java.time.LocalTime;

/**
 * Sua 1 buoi hoc le (gio / thoi luong / phong / GV / ten buoi).
 * Quy uoc PATCH: field null = KHONG doi.
 *
 * <p><b>CO Y KHONG co `topicId` o day.</b> Vi quy uoc "null = khong doi" nen
 * neu de topicId o day thi khong con cach nao GO chuyen de ve null. Moi thao
 * tac gan/go chuyen de — ke ca cho 1 buoi — di qua endpoint rieng:
 * {@code PATCH /api/v1/classes/{classId}/sessions/bulk-topic} voi dung 1 phan
 * tu trong sessionIds; o do {@code topicId = null} co nghia ro rang la "go".
 * Dung them field topicId vao day (xem SPEC_KhoaHoc_NoiDung_Mobile.md §3.4a).
 */
public record UpdateSessionRequest(
        LocalTime startTime,
        Integer durationMinutes,
        Long roomId,
        Long teacherId,
        String title
) {
}
