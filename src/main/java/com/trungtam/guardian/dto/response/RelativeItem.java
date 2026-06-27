package com.trungtam.guardian.dto.response;

import com.trungtam.guardian.entity.GuardianRelationship;
import com.trungtam.identity.entity.User;

/**
 * Mot nguoi co lien quan (phu huynh hoac hoc vien) kem quan he (neu co).
 */
public record RelativeItem(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        String status,
        String relationship
) {
    public static RelativeItem of(User user, GuardianRelationship relationship) {
        return new RelativeItem(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus() != null ? user.getStatus().name() : null,
                relationship != null ? relationship.name() : null
        );
    }
}
