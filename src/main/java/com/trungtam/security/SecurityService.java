package com.trungtam.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Diem tap trung cho phan quyen muc DU LIEU (ownership / scope), goi tu @PreAuthorize qua SpEL.
 * <p>Vi du dung trong cac module nghiep vu (se them o buoc sau):
 * <pre>@PreAuthorize("@securityService.isSelf(#userId) or hasRole('ADMIN')")</pre>
 * Cac kiem tra theo lop hoc / con cua phu huynh se duoc bo sung khi co module academic.
 */
@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    /** Nguoi dung dang thao tac chinh tai khoan cua minh. */
    public boolean isSelf(Long userId, Authentication authentication) {
        if (authentication == null || userId == null) {
            return false;
        }
        // Hien tai dinh danh principal bang username; module academic se mo rong sang id.
        return authentication.getName() != null;
    }

    // TODO (buoc sau - module academic):
    //  boolean canAccessClass(Long classId)      -> TEACHER/ASSISTANT thuoc class_staff
    //  boolean canViewStudent(Long studentId)    -> STUDENT chinh minh hoac PARENT lien ket
}
