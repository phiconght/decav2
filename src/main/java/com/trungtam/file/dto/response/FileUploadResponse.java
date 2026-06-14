package com.trungtam.file.dto.response;

import com.trungtam.file.entity.StoredFile;

public record FileUploadResponse(
        Long id,
        String url,
        String fileName,
        long size,
        String contentType
) {
    public static FileUploadResponse of(StoredFile f, String url) {
        return new FileUploadResponse(f.getId(), url, f.getOriginalName(), f.getSizeBytes(), f.getContentType());
    }
}
