package com.trungtam.room.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.room.dto.request.CreateRoomRequest;
import com.trungtam.room.dto.request.RoomSearchParams;
import com.trungtam.room.dto.response.RoomItem;
import com.trungtam.room.dto.response.RoomPageResponse;
import com.trungtam.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROOM:READ')")
    public RoomPageResponse search(@ModelAttribute RoomSearchParams params) {
        return roomService.search(params);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM:READ')")
    public ApiResponse<RoomItem> getById(@PathVariable Long id) {
        return ApiResponse.ok(roomService.getById(id));
    }

    /** Phong dang hoat dong cua 1 co so (dropdown). */
    @GetMapping("/by-branch/{branchId}")
    @PreAuthorize("hasAuthority('ROOM:READ')")
    public ApiResponse<List<RoomItem>> listByBranch(@PathVariable Long branchId) {
        return ApiResponse.ok(roomService.listByBranch(branchId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROOM:WRITE')")
    public ApiResponse<RoomItem> create(@Valid @RequestBody CreateRoomRequest request) {
        return ApiResponse.ok(roomService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM:WRITE')")
    public ApiResponse<RoomItem> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateRoomRequest request) {
        return ApiResponse.ok(roomService.update(id, request));
    }
}
