package com.trungtam.leave.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.leave.dto.request.CreateLeaveRequest;
import com.trungtam.leave.dto.request.LeaveSearchParams;
import com.trungtam.leave.dto.response.LeaveItem;
import com.trungtam.leave.dto.response.LeavePageResponse;
import com.trungtam.leave.entity.LeaveRequest;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.leave.entity.LeaveScope;
import com.trungtam.leave.entity.LeaveStatus;
import com.trungtam.leave.repository.LeaveRequestRepository;
import com.trungtam.notification.entity.NotificationType;
import com.trungtam.notification.service.NotificationService;
import com.trungtam.schedule.entity.AttendanceStatus;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionAttendance;
import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schedule.repository.SessionAttendanceRepository;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Nghiep vu don xin nghi: tao / duyet / tu choi / liet ke + tien ich isOnLeave cho job.
 * Duyet -> ap dat trang thai diem danh CO_PHEP cho cac buoi lien quan da materialize.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassSessionRepository classSessionRepository;
    private final SessionAttendanceRepository sessionAttendanceRepository;
    private final StudentParentRepository studentParentRepository;
    private final NotificationService notificationService;

    public LeavePageResponse list(LeaveSearchParams params) {
        Specification<LeaveRequest> spec = buildSpec(params);
        Sort sort = resolveSort(params.getSortField(), params.getSortOrder());
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<LeaveItem> result = leaveRequestRepository.findAll(spec, pageable).map(LeaveItem::from);
        return LeavePageResponse.of(result);
    }

    @Transactional
    public LeaveItem create(CreateLeaveRequest req) {
        User student = findUserOrThrow(req.studentId());

        ClassSession session = null;
        SchoolClass clazz = null;
        if (req.scope() == LeaveScope.SESSION) {
            if (req.sessionId() == null) {
                throw new AppException(ErrorCode.LEAVE_INVALID_RANGE);
            }
            session = classSessionRepository.findById(req.sessionId())
                    .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        } else { // RANGE
            if (req.dateFrom() == null || req.dateTo() == null || req.dateFrom().isAfter(req.dateTo())) {
                throw new AppException(ErrorCode.LEAVE_INVALID_RANGE);
            }
            if (req.classId() != null) {
                clazz = schoolClassRepository.findById(req.classId())
                        .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
            }
        }

        LeaveRequest entity = new LeaveRequest();
        entity.setStudent(student);
        entity.setRequestedBy(currentUser());
        entity.setScope(req.scope());
        entity.setSession(session);
        entity.setClazz(clazz);
        if (req.scope() == LeaveScope.RANGE) {
            entity.setDateFrom(req.dateFrom());
            entity.setDateTo(req.dateTo());
        }
        entity.setReason(req.reason());
        entity.setStatus(LeaveStatus.PENDING);
        return LeaveItem.from(leaveRequestRepository.save(entity));
    }

    @Transactional
    public LeaveItem approve(Long id) {
        LeaveRequest leave = findOrThrow(id);
        ensureNotReviewed(leave);

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setReviewedBy(currentUser());
        leave.setReviewedAt(Instant.now());
        LeaveRequest saved = leaveRequestRepository.save(leave);

        applyLeaveAttendance(saved);
        notifyLeaveResult(saved, true);
        return LeaveItem.from(saved);
    }

    @Transactional
    public LeaveItem reject(Long id) {
        LeaveRequest leave = findOrThrow(id);
        ensureNotReviewed(leave);

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setReviewedBy(currentUser());
        leave.setReviewedAt(Instant.now());
        LeaveRequest saved = leaveRequestRepository.save(leave);
        notifyLeaveResult(saved, false);
        return LeaveItem.from(saved);
    }

    /**
     * Phat LEAVE_RESULT cho hoc vien va tung phu huynh (idempotent qua dedupeKey).
     * enqueue chay REQUIRES_NEW nen an toan goi giua giao dich duyet/tu choi.
     */
    private void notifyLeaveResult(LeaveRequest leave, boolean approved) {
        Long leaveId = leave.getId();
        Long studentId = leave.getStudent().getId();
        String title = approved ? "Don nghi duoc duyet" : "Don nghi bi tu choi";
        String body = approved
                ? "Don xin nghi cua hoc vien da duoc duyet."
                : "Don xin nghi cua hoc vien da bi tu choi.";
        String payload = "{\"leaveId\":" + leaveId + ",\"approved\":" + approved + "}";

        // Hoc vien
        notificationService.enqueue(studentId, NotificationType.LEAVE_RESULT, title, body, payload,
                "LEAVE_RESULT:" + leaveId + ":" + studentId);
        // Tung phu huynh
        for (Long parentId : studentParentRepository.findParentIdsByStudentId(studentId)) {
            notificationService.enqueue(parentId, NotificationType.LEAVE_RESULT, title, body, payload,
                    "LEAVE_RESULT:" + leaveId + ":" + parentId);
        }
    }

    /**
     * HV co don nghi da DUYET cho buoi nay khong? Dung boi job activate / quet thieu check-in.
     */
    public boolean isOnLeave(Long studentId, Long sessionId) {
        ClassSession session = classSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
        Long classId = session.getClazz() != null ? session.getClazz().getId() : null;
        return leaveRequestRepository.isOnLeave(
                studentId, sessionId, session.getSessionDate(), classId, LeaveStatus.APPROVED);
    }

    // ------------------------------------------------------------------

    /** Ap CO_PHEP len cac buoi lien quan da materialize. */
    private void applyLeaveAttendance(LeaveRequest leave) {
        Long studentId = leave.getStudent().getId();
        List<ClassSession> sessions = new ArrayList<>();

        if (leave.getScope() == LeaveScope.SESSION) {
            if (leave.getSession() != null) {
                sessions.add(leave.getSession());
            }
        } else { // RANGE
            Long classId = leave.getClazz() != null ? leave.getClazz().getId() : null;
            if (classId != null) {
                sessions.addAll(classSessionRepository.findByClazzIdAndSessionDateBetween(
                        classId, leave.getDateFrom(), leave.getDateTo()));
            } else {
                // Tat ca lop: cac buoi cua tat ca lop ma HV ghi danh trong khoang ngay.
                List<SchoolClass> classes = schoolClassRepository.findClassesByStudentId(studentId);
                for (SchoolClass c : classes) {
                    sessions.addAll(classSessionRepository.findByClazzIdAndSessionDateBetween(
                            c.getId(), leave.getDateFrom(), leave.getDateTo()));
                }
            }
        }

        for (ClassSession session : sessions) {
            SessionAttendance att = sessionAttendanceRepository
                    .findBySessionIdAndUserId(session.getId(), studentId)
                    .orElseGet(() -> {
                        SessionAttendance a = new SessionAttendance();
                        a.setSession(session);
                        a.setUser(leave.getStudent());
                        return a;
                    });
            att.setStatus(AttendanceStatus.CO_PHEP);
            sessionAttendanceRepository.save(att);
        }
    }

    private void ensureNotReviewed(LeaveRequest leave) {
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new AppException(ErrorCode.LEAVE_ALREADY_REVIEWED);
        }
    }

    private LeaveRequest findOrThrow(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.LEAVE_NOT_FOUND));
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private User currentUser() {
        String username = SecurityUtils.requireCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Specification<LeaveRequest> buildSpec(LeaveSearchParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (params.getStudentId() != null) {
                predicates.add(cb.equal(root.get("student").get("id"), params.getStudentId()));
            }
            if (StringUtils.hasText(params.getStatus())) {
                predicates.add(cb.equal(root.get("status"),
                        LeaveStatus.valueOf(params.getStatus().trim().toUpperCase())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort resolveSort(String field, String order) {
        String sortField = switch (StringUtils.hasText(field) ? field : "createdAt") {
            case "status" -> "status";
            case "createdAt" -> "createdAt";
            default -> "createdAt";
        };
        Sort.Direction dir = "ascend".equals(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, sortField);
    }
}
