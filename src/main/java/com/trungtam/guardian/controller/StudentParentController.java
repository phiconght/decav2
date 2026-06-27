package com.trungtam.guardian.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.guardian.dto.request.LinkParentRequest;
import com.trungtam.guardian.dto.response.RelativeItem;
import com.trungtam.guardian.service.StudentParentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class StudentParentController {

    private final StudentParentService studentParentService;

    @GetMapping("/students/{id}/parents")
    @PreAuthorize("hasAuthority('USER:READ')")
    public ApiResponse<List<RelativeItem>> listParents(@PathVariable Long id) {
        return ApiResponse.ok(studentParentService.listParents(id));
    }

    @PostMapping("/students/{id}/parents")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ApiResponse<Void> linkParent(
            @PathVariable Long id,
            @Valid @RequestBody LinkParentRequest request) {
        studentParentService.link(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/students/{id}/parents/{parentId}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ApiResponse<Void> unlinkParent(
            @PathVariable Long id,
            @PathVariable Long parentId) {
        studentParentService.unlink(id, parentId);
        return ApiResponse.ok();
    }

    @GetMapping("/parents/{id}/children")
    @PreAuthorize("hasAuthority('USER:READ')")
    public ApiResponse<List<RelativeItem>> listChildren(@PathVariable Long id) {
        return ApiResponse.ok(studentParentService.listChildren(id));
    }

    @GetMapping("/parents/options")
    @PreAuthorize("hasAuthority('USER:READ')")
    public ApiResponse<List<RelativeItem>> parentOptions(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(studentParentService.parentOptions(keyword));
    }
}
