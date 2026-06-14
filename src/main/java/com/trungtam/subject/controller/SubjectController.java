package com.trungtam.subject.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.subject.dto.request.SubjectSearchParams;
import com.trungtam.subject.dto.response.SubjectDetailResponse;
import com.trungtam.subject.dto.response.SubjectPageResponse;
import com.trungtam.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUBJECT:READ')")
    public SubjectPageResponse search(@ModelAttribute SubjectSearchParams params) {
        return subjectService.search(params);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBJECT:READ')")
    public ApiResponse<SubjectDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(subjectService.getById(id));
    }
}
