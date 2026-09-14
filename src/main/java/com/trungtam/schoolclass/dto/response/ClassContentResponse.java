package com.trungtam.schoolclass.dto.response;

import com.trungtam.schoolclass.entity.ClassMarketingContent;

/** Noi dung hien thi khoa hoc — cho Admin nap vao popup "Nội dung" khi mo. */
public record ClassContentResponse(
        Long classId,
        String title,
        String coverImageUrl,
        String contentMd
) {
    /** Chua tung tao noi dung — tra rong de popup mo trang, khong loi 404. */
    public static ClassContentResponse empty(Long classId) {
        return new ClassContentResponse(classId, null, null, null);
    }

    public static ClassContentResponse from(ClassMarketingContent c) {
        return new ClassContentResponse(c.getClassId(), c.getTitle(), c.getCoverImageUrl(), c.getContentMd());
    }
}
