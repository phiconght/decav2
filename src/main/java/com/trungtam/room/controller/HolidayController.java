package com.trungtam.room.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.room.dto.request.CreateHolidayRequest;
import com.trungtam.room.dto.response.HolidayItem;
import com.trungtam.room.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROOM:READ')")
    public ApiResponse<List<HolidayItem>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(holidayService.list(from, to));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROOM:WRITE')")
    public ApiResponse<HolidayItem> create(@Valid @RequestBody CreateHolidayRequest request) {
        return ApiResponse.ok(holidayService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROOM:WRITE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        holidayService.delete(id);
        return ApiResponse.ok();
    }
}
