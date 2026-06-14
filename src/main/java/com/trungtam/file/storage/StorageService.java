package com.trungtam.file.storage;

import java.io.InputStream;

public interface StorageService {
    void store(String relativePath, byte[] content);
    InputStream load(String relativePath);
    void delete(String relativePath);
    boolean exists(String relativePath);
}
