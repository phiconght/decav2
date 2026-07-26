package com.trungtam.security;

import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Diem tap trung cho phan quyen muc DU LIEU (ownership / scope), goi tu @PreAuthorize qua SpEL.
 * <p>Vi du:
 * <pre>@PreAuthorize("hasAuthority('REPORT:READ') and @securityService.canViewStudentReport(#studentId, authentication)")</pre>
 * <p>Luu y: principal la USERNAME (chuoi) do JwtAuthenticationFilter dat — resolve id qua UserRepository.
 */
@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final StudentParentRepository studentParentRepository;
    private final SchoolClassRepository schoolClassRepository;

    /** Nguoi dung dang thao tac chinh tai khoan cua minh. */
    public boolean isSelf(Long userId, Authentication authentication) {
        Long myId = currentUserId(authentication);
        return myId != null && myId.equals(userId);
    }

    /**
     * Ai duoc xem bao cao cua 1 hoc vien:
     * - ADMIN / EMPLOYEE: tat ca.
     * - STUDENT: chinh minh.
     * - PARENT: co lien ket student_parents.
     * - TEACHER / ASSISTANT: co chung lop voi hoc vien.
     */
    public boolean canViewStudentReport(Long studentId, Authentication authentication) {
        if (authentication == null || studentId == null) {
            return false;
        }
        Set<String> roles = roles(authentication);
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE")) {
            return true;
        }
        Long myId = currentUserId(authentication);
        if (myId == null) {
            return false;
        }
        if (roles.contains("ROLE_STUDENT") && myId.equals(studentId)) {
            return true;
        }
        if (roles.contains("ROLE_PARENT")
                && studentParentRepository.findByStudentIdAndParentId(studentId, myId).isPresent()) {
            return true;
        }
        if (roles.contains("ROLE_TEACHER") || roles.contains("ROLE_ASSISTANT")) {
            return schoolClassRepository.teacherSharesClassWithStudent(myId, studentId);
        }
        return false;
    }

    /**
     * Ai duoc xem bao cao cap LOP:
     * - ADMIN / EMPLOYEE: tat ca.
     * - TEACHER / ASSISTANT: lop minh day.
     */
    public boolean canAccessClass(Long classId, Authentication authentication) {
        if (authentication == null || classId == null) {
            return false;
        }
        Set<String> roles = roles(authentication);
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE")) {
            return true;
        }
        Long myId = currentUserId(authentication);
        if (myId == null) {
            return false;
        }
        if (roles.contains("ROLE_TEACHER") || roles.contains("ROLE_ASSISTANT")) {
            return schoolClassRepository.existsByIdAndTeachers_Id(classId, myId);
        }
        return false;
    }

    /**
     * Ai duoc xem NOI DUNG cua 1 khoa hoc (buoi hoc, de thi, chuyen de):
     * - ADMIN / EMPLOYEE: tat ca.
     * - TEACHER / ASSISTANT: lop minh day.
     * - STUDENT: lop minh dang hoc.
     * - PARENT: lop ma it nhat 1 con dang hoc.
     *
     * <p>KHAC {@link #canAccessClass}: ham do danh cho bao cao CAP LOP (thong ke
     * ca lop) nen chan STUDENT/PARENT. Ham nay danh cho noi dung khoa hoc ma
     * chinh hoc vien/phu huynh phai xem duoc.
     *
     * <p>Dung chung cho {@code GET /classes/{id}/outline} va
     * {@code GET /exams/by-class/{classId}} — xem
     * SPEC_KhoaHoc_NoiDung_Mobile.md §3.3 va §3.5.
     */
    public boolean canViewClassContent(Long classId, Authentication authentication) {
        if (authentication == null || classId == null) {
            return false;
        }
        Set<String> roles = roles(authentication);
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE")) {
            return true;
        }
        Long myId = currentUserId(authentication);
        if (myId == null) {
            return false;
        }
        if ((roles.contains("ROLE_TEACHER") || roles.contains("ROLE_ASSISTANT"))
                && schoolClassRepository.existsByIdAndTeachers_Id(classId, myId)) {
            return true;
        }
        if (roles.contains("ROLE_STUDENT")
                && schoolClassRepository.existsByIdAndStudents_Id(classId, myId)) {
            return true;
        }
        if (roles.contains("ROLE_PARENT")) {
            return studentParentRepository.findByParentIdOrderByIdAsc(myId).stream()
                    .anyMatch(sp -> schoolClassRepository
                            .existsByIdAndStudents_Id(classId, sp.getStudent().getId()));
        }
        return false;
    }

    /** Id nguoi dung hien tai (resolve tu username trong principal). */
    public Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName())
                .map(User::getId)
                .orElse(null);
    }

    private Set<String> roles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
