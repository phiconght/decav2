package com.trungtam.exam.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.dto.request.ExamConfirmationSearchParams;
import com.trungtam.exam.dto.response.ExamConfirmationItem;
import com.trungtam.exam.dto.response.ExamConfirmationPageResponse;
import com.trungtam.exam.entity.ExamStudent;
import com.trungtam.exam.entity.ExamStudentStatus;
import com.trungtam.exam.repository.ExamStudentRepository;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Xac nhan bai thi da nop (DA_LAM) truoc khi tinh vao bao cao — yeu cau
 * nguoi dung 13/08/2026. Cho phep xac nhan tung bai hoac hang loat.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExamConfirmationService {

    private final ExamStudentRepository examStudentRepository;
    private final UserRepository userRepository;

    public ExamConfirmationPageResponse listPending(ExamConfirmationSearchParams params) {
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size);
        var result = examStudentRepository
                .findPendingConfirmation(params.getExamId(), params.getClassId(), pageable)
                .map(ExamConfirmationItem::from);
        return ExamConfirmationPageResponse.of(result);
    }

    @Transactional
    public ExamConfirmationItem confirm(Long examStudentId) {
        ExamStudent es = examStudentRepository.findById(examStudentId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_STUDENT_NOT_FOUND));
        doConfirm(es, currentUser());
        return ExamConfirmationItem.from(es);
    }

    /** Xac nhan hang loat — bo qua id khong o trang thai DA_LAM/da xac nhan roi (khong loi ca lo). */
    @Transactional
    public int confirmBulk(List<Long> examStudentIds) {
        User me = currentUser();
        int confirmed = 0;
        for (ExamStudent es : examStudentRepository.findByIdIn(examStudentIds)) {
            if (es.getStatus() == ExamStudentStatus.DA_LAM && es.getConfirmedAt() == null) {
                doConfirm(es, me);
                confirmed++;
            }
        }
        return confirmed;
    }

    private void doConfirm(ExamStudent es, User by) {
        if (es.getStatus() != ExamStudentStatus.DA_LAM) {
            throw new AppException(ErrorCode.EXAM_NOT_SUBMITTED);
        }
        if (es.getConfirmedAt() != null) {
            throw new AppException(ErrorCode.EXAM_ALREADY_CONFIRMED);
        }
        es.setConfirmedBy(by);
        es.setConfirmedAt(Instant.now());
        examStudentRepository.save(es);
    }

    private User currentUser() {
        String username = SecurityUtils.requireCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
