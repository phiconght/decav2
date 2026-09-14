package com.trungtam.schoolclass.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.schoolclass.dto.request.UpdateClassContentRequest;
import com.trungtam.schoolclass.dto.response.ClassContentResponse;
import com.trungtam.schoolclass.entity.ClassMarketingContent;
import com.trungtam.schoolclass.repository.ClassMarketingContentRepository;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Noi dung hien thi (title/anh/bai gioi thieu) cua 1 khoa hoc — nut "Nội dung"
 * trong man Sua khoa (Admin). Tach khoi {@link ClassService} vi day la moi
 * quan tam rieng (hien thi), khong dinh gi den gia/lich hoc.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClassContentService {

    private final ClassMarketingContentRepository repository;
    private final SchoolClassRepository classRepository;

    /** Chua tung tao -> tra rong (khong 404) de popup Admin mo trang lan dau. */
    public ClassContentResponse get(Long classId) {
        requireClassExists(classId);
        return repository.findById(classId)
                .map(ClassContentResponse::from)
                .orElseGet(() -> ClassContentResponse.empty(classId));
    }

    @Transactional
    public ClassContentResponse update(Long classId, UpdateClassContentRequest req) {
        requireClassExists(classId);
        ClassMarketingContent content = repository.findById(classId)
                .orElseGet(() -> {
                    ClassMarketingContent c = new ClassMarketingContent();
                    c.setClassId(classId);
                    return c;
                });
        content.setTitle(req.title());
        content.setCoverImageUrl(req.coverImageUrl());
        content.setContentMd(req.contentMd());
        return ClassContentResponse.from(repository.save(content));
    }

    private void requireClassExists(Long classId) {
        if (!classRepository.existsById(classId)) {
            throw new AppException(ErrorCode.CLASS_NOT_FOUND);
        }
    }
}
