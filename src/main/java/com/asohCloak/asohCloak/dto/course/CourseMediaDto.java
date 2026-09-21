package com.asohCloak.asohCloak.dto.course;
import com.asohCloak.asohCloak.entity.courseMedia.CourseMedia;
import com.asohCloak.asohCloak.enums.MediaKind;
import com.asohCloak.asohCloak.enums.MediaStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A file attached to a course, with its upload state")
public record CourseMediaDto(
        UUID id,
        String fileName,
        MediaKind kind,
        String contentType,
        long sizeBytes,
        Integer durationSeconds,
        @Schema(description = "UPLOADING, READY, FAILED or ABORTED") MediaStatus status,
        @Schema(description = "Download link, only present once status is READY") String url,
        Instant createdAt,
        Instant completedAt
) {
    public static CourseMediaDto from(CourseMedia media) {
        return new CourseMediaDto(
                media.getId(), media.getFileName(), media.getKind(), media.getContentType(),
                media.getSizeBytes(), media.getDurationSeconds(), media.getStatus(),
                null, media.getCreatedAt(), media.getCompletedAt());
    }
}