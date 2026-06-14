package com.trungtam.file.controller;

import com.trungtam.common.dto.ApiResponse;
import com.trungtam.file.dto.response.FileUploadResponse;
import com.trungtam.file.entity.StoredFile;
import com.trungtam.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FILE:WRITE')")
    public ApiResponse<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false) String folder) {
        return ApiResponse.ok(fileService.upload(file, folder));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> content(@PathVariable Long id) {
        StoredFile f = fileService.getActive(id);
        InputStreamResource body = new InputStreamResource(fileService.loadContent(f));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.getContentType()))
                .contentLength(f.getSizeBytes())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + f.getOriginalName() + "\"")
                .body(body);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FILE:DELETE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        fileService.softDelete(id);
        return ApiResponse.ok();
    }
}
