package com.trungtam.guardian.dto.request;

import com.trungtam.guardian.entity.GuardianRelationship;
import jakarta.validation.constraints.NotNull;

/**
 * Yeu cau gan / cap nhat 1 phu huynh cho hoc vien.
 */
public record LinkParentRequest(
        @NotNull Long parentId,
        GuardianRelationship relationship
) {
}
