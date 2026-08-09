package com.trungtam.report.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.entity.ExamStudent;
import com.trungtam.exam.repository.ExamStudentRepository;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.report.dto.request.CreateCommentRequest;
import com.trungtam.report.dto.request.UpdateCommentRequest;
import com.trungtam.report.dto.response.CommentItem;
import com.trungtam.report.entity.ReportComment;
import com.trungtam.report.repository.ReportCommentRepository;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Nhan xet bao cao: liet ke (loc theo quyen), them, sua, xoa.
 * Quyen doc/scope da kiem o @PreAuthorize (canViewStudentReport); day
 * chi bo sung: HS xem chinh minh -> chi thay nhan xet visibleToStudent=true;
 * sua/xoa -> chi tac gia (xoa: them ADMIN).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportCommentService {

    /** Uu tien vai tro khi snapshot author_role. */
    private static final List<String> ROLE_PRIORITY =
            List.of("ADMIN", "TEACHER", "ASSISTANT", "PARENT", "EMPLOYEE");

    private final ReportCommentRepository reportCommentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ExamStudentRepository examStudentRepository;
    private final UserRepository userRepository;

    public List<CommentItem> list(Long studentId, Long classId, Long examStudentId) {
        List<ReportComment> rows;
        if (examStudentId != null) {
            // @PreAuthorize chi kiem tra #studentId — neu khong doi chieu o day,
            // truyen dung studentId cua minh + examStudentId cua HV khac se doc
            // duoc nhan xet cua HV do (IDOR).
            ExamStudent es = examStudentRepository.findById(examStudentId)
                    .orElseThrow(() -> new AppException(ErrorCode.REPORT_COMMENT_NOT_FOUND));
            if (!es.getUser().getId().equals(studentId)) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
            rows = reportCommentRepository.findByExamStudentIdOrderByCreatedAtDesc(examStudentId);
        } else {
            rows = reportCommentRepository.findByStudentIdAndSchoolClassIdOrderByCreatedAtDesc(studentId, classId);
        }

        boolean studentSelf = isStudentViewingSelf(studentId);
        return rows.stream()
                .filter(c -> !studentSelf || c.isVisibleToStudent())
                .map(CommentItem::from)
                .toList();
    }

    @Transactional
    public CommentItem create(CreateCommentRequest req) {
        if (!schoolClassRepository.existsByIdAndStudents_Id(req.classId(), req.studentId())) {
            throw new AppException(ErrorCode.STUDENT_NOT_IN_CLASS);
        }
        ExamStudent examStudent = null;
        if (req.examStudentId() != null) {
            examStudent = examStudentRepository.findById(req.examStudentId())
                    .orElseThrow(() -> new AppException(ErrorCode.REPORT_COMMENT_NOT_FOUND));
            if (!examStudent.getUser().getId().equals(req.studentId())) {
                throw new AppException(ErrorCode.STUDENT_NOT_IN_CLASS);
            }
            boolean examInClass = examStudent.getExam().getClasses().stream()
                    .anyMatch(c -> c.getId().equals(req.classId()));
            if (!examInClass) {
                throw new AppException(ErrorCode.EXAM_NOT_IN_CLASS);
            }
        }

        User author = currentUser();
        SchoolClass clazz = schoolClassRepository.findById(req.classId())
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
        User student = userRepository.findById(req.studentId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ReportComment c = new ReportComment();
        c.setStudent(student);
        c.setSchoolClass(clazz);
        c.setExamStudent(examStudent);
        c.setAuthor(author);
        c.setAuthorRole(highestRole());
        c.setContent(req.content());
        c.setVisibleToStudent(req.visibleToStudent());
        return CommentItem.from(reportCommentRepository.save(c));
    }

    @Transactional
    public CommentItem update(Long id, UpdateCommentRequest req) {
        ReportComment c = reportCommentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_COMMENT_NOT_FOUND));
        User me = currentUser();
        if (!c.getAuthor().getId().equals(me.getId())) {
            throw new AppException(ErrorCode.REPORT_COMMENT_FORBIDDEN);
        }
        c.setContent(req.content());
        if (req.visibleToStudent() != null) {
            c.setVisibleToStudent(req.visibleToStudent());
        }
        return CommentItem.from(reportCommentRepository.save(c));
    }

    @Transactional
    public void delete(Long id) {
        ReportComment c = reportCommentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_COMMENT_NOT_FOUND));
        User me = currentUser();
        boolean admin = roles().contains("ROLE_ADMIN");
        if (!admin && !c.getAuthor().getId().equals(me.getId())) {
            throw new AppException(ErrorCode.REPORT_COMMENT_FORBIDDEN);
        }
        reportCommentRepository.delete(c);
    }

    // ---- helpers ----

    private boolean isStudentViewingSelf(Long studentId) {
        Set<String> roles = roles();
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_EMPLOYEE")
                || roles.contains("ROLE_TEACHER") || roles.contains("ROLE_ASSISTANT")
                || roles.contains("ROLE_PARENT")) {
            return false;
        }
        // Chi con vai tro STUDENT
        return currentUser().getId().equals(studentId);
    }

    private String highestRole() {
        Set<String> roles = roles();
        for (String r : ROLE_PRIORITY) {
            if (roles.contains("ROLE_" + r)) {
                return r;
            }
        }
        return "USER";
    }

    private User currentUser() {
        return userRepository.findByUsername(SecurityUtils.requireCurrentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Set<String> roles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
