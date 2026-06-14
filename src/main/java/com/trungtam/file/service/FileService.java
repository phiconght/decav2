package com.trungtam.file.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.file.config.StorageProperties;
import com.trungtam.file.dto.response.FileUploadResponse;
import com.trungtam.file.entity.FileStatus;
import com.trungtam.file.entity.StoredFile;
import com.trungtam.file.repository.StoredFileRepository;
import com.trungtam.file.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final StorageService storageService;
    private final StoredFileRepository repository;
    private final StorageProperties props;

    @Transactional
    public FileUploadResponse upload(MultipartFile file, String folder) {
        byte[] bytes = readBytes(file);
        validate(file, bytes);

        String checksum = sha256(bytes);
        // dedup: neu cung file da ACTIVE, tra URL cu
        var existing = repository.findByChecksumAndStatus(checksum, FileStatus.ACTIVE);
        if (existing.isPresent()) {
            StoredFile dup = existing.get();
            return FileUploadResponse.of(dup, buildUrl(dup));
        }

        String ext = resolveExtension(file);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String storedName = uuid + "." + ext;
        String relativePath = datePrefix() + "/" + storedName;

        storageService.store(relativePath, bytes);

        StoredFile entity = new StoredFile();
        entity.setOriginalName(sanitizeFileName(file.getOriginalFilename()));
        entity.setStoredName(storedName);
        entity.setRelativePath(relativePath);
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setChecksum(checksum);
        entity.setFolder(folder);
        StoredFile saved = repository.save(entity);

        return FileUploadResponse.of(saved, buildUrl(saved));
    }

    @Transactional(readOnly = true)
    public StoredFile getActive(Long id) {
        StoredFile f = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.FILE_NOT_FOUND));
        if (f.getStatus() == FileStatus.DELETED) throw new AppException(ErrorCode.FILE_NOT_FOUND);
        return f;
    }

    public InputStream loadContent(StoredFile f) {
        return storageService.load(f.getRelativePath());
    }

    @Transactional
    public void softDelete(Long id) {
        StoredFile f = getActive(id);
        f.setStatus(FileStatus.DELETED);
        repository.save(f);
    }

    // ---- helpers ----

    private void validate(MultipartFile file, byte[] bytes) {
        if (file.isEmpty() || bytes.length == 0) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }
        if (bytes.length > props.maxFileSizeBytes()) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }
        String ct = file.getContentType();
        List<String> allowed = props.allowedContentTypes();
        if (ct == null || !allowed.contains(ct)) {
            throw new AppException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (!magicBytesMatch(bytes, ct)) {
            throw new AppException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private boolean magicBytesMatch(byte[] b, String contentType) {
        return switch (contentType) {
            case "image/png"  -> b.length >= 4
                    && (b[0] & 0xFF) == 0x89 && (b[1] & 0xFF) == 0x50
                    && (b[2] & 0xFF) == 0x4E && (b[3] & 0xFF) == 0x47;
            case "image/jpeg" -> b.length >= 3
                    && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8
                    && (b[2] & 0xFF) == 0xFF;
            case "image/gif"  -> b.length >= 4
                    && (b[0] & 0xFF) == 0x47 && (b[1] & 0xFF) == 0x49
                    && (b[2] & 0xFF) == 0x46 && (b[3] & 0xFF) == 0x38;
            case "image/webp" -> b.length >= 12
                    && (b[0] & 0xFF) == 0x52 && (b[1] & 0xFF) == 0x49
                    && (b[2] & 0xFF) == 0x46 && (b[3] & 0xFF) == 0x46
                    && (b[8] & 0xFF) == 0x57 && (b[9] & 0xFF) == 0x45
                    && (b[10] & 0xFF) == 0x42 && (b[11] & 0xFF) == 0x50;
            default -> false;
        };
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR, "Khong doc duoc file: " + e.getMessage());
        }
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveExtension(MultipartFile file) {
        String ct = file.getContentType();
        if (ct == null) return "bin";
        return switch (ct) {
            case "image/png"  -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif"  -> "gif";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }

    private String datePrefix() {
        LocalDate d = LocalDate.now(ZoneOffset.UTC);
        return "%04d/%02d".formatted(d.getYear(), d.getMonthValue());
    }

    private String buildUrl(StoredFile f) {
        String base = props.publicBaseUrl() == null ? "" : props.publicBaseUrl();
        return base + "/api/v1/files/" + f.getId() + "/content";
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^\\w.\\-]", "_");
    }
}
