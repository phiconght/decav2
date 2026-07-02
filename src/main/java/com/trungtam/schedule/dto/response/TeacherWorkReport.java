package com.trungtam.schedule.dto.response;

import java.util.List;

/** Bao cao cong GV: tong ket + danh sach buoi. */
public record TeacherWorkReport(
        Summary summary,
        List<TeacherWorkItem> items
) {
    public record Summary(
            int totalSessions,
            int dungGio,
            int vaoTre,
            int vang,
            int chuaCham,
            int totalTaughtMinutes   // tong thoi luong buoi DUNG_GIO + VAO_TRE
    ) {
    }
}
