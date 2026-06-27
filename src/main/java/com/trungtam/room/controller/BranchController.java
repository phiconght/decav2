package com.trungtam.room.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.room.dto.request.CreateBranchRequest;
import com.trungtam.room.dto.response.BranchItem;
import com.trungtam.room.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROOM:READ')")
    public ApiResponse<List<BranchItem>> list(
            @RequestParam(name = "all", required = false, defaultValue = "false") boolean all) {
        return ApiResponse.ok(all ? branchService.listAll() : branchService.listActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM:READ')")
    public ApiResponse<BranchItem> getById(@PathVariable Long id) {
        return ApiResponse.ok(branchService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROOM:WRITE')")
    public ApiResponse<BranchItem> create(@Valid @RequestBody CreateBranchRequest request) {
        return ApiResponse.ok(branchService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM:WRITE')")
    public ApiResponse<BranchItem> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateBranchRequest request) {
        return ApiResponse.ok(branchService.update(id, request));
    }
}
