package com.asohCloak.asohCloak.dto.course;

import com.asohCloak.asohCloak.enums.MediaKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MediaUploadSessionDto(
        UUID mediaId,
        String fileName,
        MediaKind kind,
        String uploadId,
        long partSizeBytes,
        int partCount,
        List<PresignedPartDto> parts,
        Instant partUrlsExpireAt
) { }