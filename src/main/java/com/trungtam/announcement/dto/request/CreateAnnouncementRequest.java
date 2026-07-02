package com.trungtam.announcement.dto.request;

import com.trungtam.announcement.entity.AudienceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Yeu cau soan + gui thong bao.
 * audienceRef: ROLE -> csv role (vd "STUDENT,PARENT"); CLASS -> classId; USERS -> csv userId; ALL -> bo trong.
 */
public record CreateAnnouncementRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String contentMd,
        @NotNull AudienceType audience,
        String audienceRef,
        Long postId
) {
}
