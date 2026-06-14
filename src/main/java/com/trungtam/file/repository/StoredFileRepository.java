package com.trungtam.file.repository;

import com.trungtam.file.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByChecksumAndStatus(String checksum, com.trungtam.file.entity.FileStatus status);
}
