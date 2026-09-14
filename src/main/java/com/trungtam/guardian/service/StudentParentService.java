package com.trungtam.guardian.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.guardian.dto.request.LinkParentRequest;
import com.trungtam.guardian.dto.response.RelativeItem;
import com.trungtam.guardian.entity.StudentParent;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quan ly lien ket Hoc vien <-> Phu huynh.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudentParentService {

    private final StudentParentRepository studentParentRepository;
    private final UserRepository userRepository;

    /** Danh sach phu huynh cua 1 hoc vien. */
    public List<RelativeItem> listParents(Long studentId) {
        return studentParentRepository.findByStudentIdOrderByIdAsc(studentId).stream()
                .map(sp -> RelativeItem.of(sp.getParent(), sp.getRelationship()))
                .toList();
    }

    /** Danh sach hoc vien (con) cua 1 phu huynh. */
    public List<RelativeItem> listChildren(Long parentId) {
        return studentParentRepository.findByParentIdOrderByIdAsc(parentId).stream()
                .map(sp -> RelativeItem.of(sp.getStudent(), sp.getRelationship()))
                .toList();
    }

    /** Picker phu huynh theo tu khoa. */
    public List<RelativeItem> parentOptions(String keyword) {
        return userRepository.findByRoleAndKeyword(RoleName.PARENT, keyword == null ? "" : keyword).stream()
                .map(u -> RelativeItem.of(u, null))
                .toList();
    }

    /**
     * Gan / cap nhat 1 phu huynh cho hoc vien (upsert idempotent theo (student,parent)).
     */
    @Transactional
    public void link(Long studentId, LinkParentRequest req) {
        User student = findUserOrThrow(studentId);
        if (!hasRole(student, RoleName.STUDENT)) {
            throw new AppException(ErrorCode.NOT_A_STUDENT);
        }
        User parent = findUserOrThrow(req.parentId());
        if (!hasRole(parent, RoleName.PARENT)) {
            throw new AppException(ErrorCode.NOT_A_PARENT);
        }

        StudentParent link = studentParentRepository
                .findByStudentIdAndParentId(studentId, req.parentId())
                .orElseGet(() -> {
                    StudentParent sp = new StudentParent();
                    sp.setStudent(student);
                    sp.setParent(parent);
                    return sp;
                });
        link.setRelationship(req.relationship());
        studentParentRepository.save(link);
    }

    /** Go lien ket (idempotent: khong co thi bo qua). */
    @Transactional
    public void unlink(Long studentId, Long parentId) {
        studentParentRepository.findByStudentIdAndParentId(studentId, parentId)
                .ifPresent(studentParentRepository::delete);
    }

    // ------------------------------------------------------------------

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean hasRole(User user, RoleName role) {
        return user.getRoles().stream().anyMatch(r -> r.getName() == role);
    }
}
