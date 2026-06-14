package com.trungtam.file.storage;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.file.config.StorageProperties;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.*;

@Service
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(StorageProperties props) {
        this.root = Paths.get(props.localRoot()).toAbsolutePath().normalize();
    }

    @Override
    public void store(String relativePath, byte[] content) {
        try {
            Path target = resolveSafe(relativePath);
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR, "Khong ghi duoc file: " + e.getMessage());
        }
    }

    @Override
    public InputStream load(String relativePath) {
        try {
            Path target = resolveSafe(relativePath);
            if (!Files.exists(target)) throw new AppException(ErrorCode.FILE_NOT_FOUND);
            return Files.newInputStream(target);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.FILE_STORAGE_ERROR, e.getMessage());
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolveSafe(relativePath));
        } catch (Exception ignored) {}
    }

    @Override
    public boolean exists(String relativePath) {
        return Files.exists(resolveSafe(relativePath));
    }

    private Path resolveSafe(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Duong dan khong hop le");
        }
        return resolved;
    }
}
