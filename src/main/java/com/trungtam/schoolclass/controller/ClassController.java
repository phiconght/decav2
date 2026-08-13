package com.trungtam.schoolclass.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.schoolclass.dto.request.AddStudentsRequest;
import com.trungtam.schoolclass.dto.request.ClassSearchParams;
import com.trungtam.schoolclass.dto.request.CreateClassRequest;
import com.trungtam.schoolclass.dto.request.UpdateClassStatusRequest;
import com.trungtam.schoolclass.dto.response.ClassDetailResponse;
import com.trungtam.schoolclass.dto.response.ClassOutlineResponse;
import com.trungtam.schoolclass.dto.response.ClassListItem;
import com.trungtam.schoolclass.dto.response.ClassPageResponse;
import com.trungtam.schoolclass.dto.response.ClassRefItem;
import com.trungtam.schoolclass.dto.response.StudentOptionResponse;
import com.trungtam.schoolclass.service.ClassOutlineService;
import com.trungtam.schoolclass.service.ClassService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassController {

    private final ClassService classService;
    private final ClassOutlineService classOutlineService;

    @GetMapping
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ClassPageResponse search(@ModelAttribute ClassSearchParams params) {
        return classService.search(params);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<ClassDetailResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(classService.getById(id));
    }

    /** Lay thong tin gon cua khoa theo list id (cho dropdown map nhan). */
    @GetMapping("/by-ids")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<List<ClassRefItem>> listByIds(
            @RequestParam(required = false) List<Long> ids) {
        return ApiResponse.ok(classService.listByIds(ids));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<ClassDetailResponse> create(@Valid @RequestBody CreateClassRequest request) {
        return ApiResponse.ok(classService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<ClassDetailResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassRequest request) {
        return ApiResponse.ok(classService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<ClassDetailResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClassStatusRequest request) {
        classService.updateStatus(id, request);
        return ApiResponse.ok(classService.getById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLASS:DELETE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        classService.delete(id);
        return ApiResponse.ok();
    }

    // ---- Ghi danh ----

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<List<StudentOptionResponse>> listStudents(@PathVariable Long id) {
        return ApiResponse.ok(classService.listStudents(id));
    }

    @GetMapping("/{id}/eligible-students")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<List<StudentOptionResponse>> listEligibleStudents(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(classService.listEligibleStudents(id, keyword));
    }

    @PostMapping("/{id}/students")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<Void> addStudents(
            @PathVariable Long id,
            @Valid @RequestBody AddStudentsRequest request) {
        classService.addStudents(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/students/{userId}")
    @PreAuthorize("hasAuthority('CLASS:WRITE')")
    public ApiResponse<Void> removeStudent(
            @PathVariable Long id,
            @PathVariable Long userId) {
        classService.removeStudent(id, userId);
        return ApiResponse.ok();
    }

    /** Danh sach lop ma 1 hoc vien dang tham gia. */
    @GetMapping("/by-student/{userId}")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<List<ClassListItem>> listByStudent(@PathVariable Long userId) {
        return ApiResponse.ok(classService.listClassesByStudent(userId));
    }

    /** Danh sach khoa hoc ma 1 giao vien phu trach. */
    @GetMapping("/by-teacher/{userId}")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<List<ClassListItem>> listByTeacher(@PathVariable Long userId) {
        return ApiResponse.ok(classService.listClassesByTeacher(userId));
    }

    /** Danh sach lop cua hoc vien DANG DANG NHAP (self-scoped, moi user da auth). */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ClassListItem>> listMyClasses() {
        return ApiResponse.ok(classService.listMyClasses());
    }

    /**
     * Cay noi dung khoa hoc: CHUYEN DE -> (buoi hoc + de thi).
     * Nguon duy nhat cho man Chi tiet khoa hoc o Mobile.
     *
     * <p>{@code isAuthenticated()} + guard trong service, KHONG dung
     * {@code CLASS:READ}: quyen do chi cap cho ADMIN/TEACHER/EMPLOYEE (V8),
     * trong khi doi tuong chinh cua man nay la STUDENT/PARENT.
     *
     * <p>{@code studentId} — BAT BUOC voi PARENT (co the co nhieu con), bi bo
     * qua voi STUDENT, tuy chon voi GV/admin. Xem SPEC §3.3.
     */
    @GetMapping("/{id}/outline")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ClassOutlineResponse> outline(
            @PathVariable Long id,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false, defaultValue = "false") boolean onlyDone) {
        return ApiResponse.ok(classOutlineService.outline(id, studentId, onlyDone));
    }

    /** Dropdown giao vien cho man xep lich (gate CLASS:READ). */
    @GetMapping("/teacher-options")
    @PreAuthorize("hasAuthority('CLASS:READ')")
    public ApiResponse<List<StudentOptionResponse>> teacherOptions(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(classService.teacherOptions(keyword));
    }
}
