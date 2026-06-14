package com.trungtam.subject.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.subject.dto.request.SubjectSearchParams;
import com.trungtam.subject.dto.response.SubjectDetailResponse;
import com.trungtam.subject.dto.response.SubjectListItem;
import com.trungtam.subject.dto.response.SubjectPageResponse;
import com.trungtam.subject.entity.Subject;
import com.trungtam.subject.repository.SubjectRepository;
import com.trungtam.subject.repository.SubjectSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectPageResponse search(SubjectSearchParams params) {
        Specification<Subject> spec = SubjectSpec.build(params);
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 200);
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("name").ascending().and(Sort.by("id").ascending()));
        Page<SubjectListItem> result = subjectRepository.findAll(spec, pageable)
                .map(SubjectListItem::from);
        return SubjectPageResponse.of(result);
    }

    public SubjectDetailResponse getById(Long id) {
        return SubjectDetailResponse.from(findOrThrow(id));
    }

    Subject findOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SUBJECT_NOT_FOUND));
    }
}
