package com.asohCloak.asohCloak.repository.storedFileRepository;

import com.asohCloak.asohCloak.entity.storeFile.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    Optional<StoredFile> findByObjectKey(String objectKey);

    List<StoredFile> findAllByExpiresAtBefore(Instant cutoff);
}