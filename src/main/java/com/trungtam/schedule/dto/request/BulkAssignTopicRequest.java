package com.trungtam.schedule.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Gan / go chuyen de cho MOT HOAC NHIEU buoi hoc cua cung 1 khoa.
 *
 * <p>Day la duong DUY NHAT de doi chuyen de cua buoi hoc — ke ca khi chi doi
 * 1 buoi (truyen dung 1 phan tu trong {@code sessionIds}). Ly do: o endpoint
 * PATCH sua buoi le, quy uoc "field null = khong doi" khien khong the GO
 * chuyen de ve null. O day thi ro rang:
 *
 * <ul>
 *   <li>{@code topicId != null} -> gan chuyen de do</li>
 *   <li>{@code topicId == null} -> GO chuyen de khoi cac buoi da chon</li>
 * </ul>
 *
 * Xem SPEC_KhoaHoc_NoiDung_Mobile.md §3.4a/§3.4c.
 */
public record BulkAssignTopicRequest(
        @NotEmpty(message = "Phai chon it nhat 1 buoi hoc")
        List<Long> sessionIds,
        Long topicId
) {
}
