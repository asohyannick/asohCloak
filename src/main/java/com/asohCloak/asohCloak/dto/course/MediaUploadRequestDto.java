package com.asohCloak.asohCloak.dto.course;

import com.asohCloak.asohCloak.enums.MediaKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.Set;

@Schema(description = "Metadata for a file the client will upload directly to storage")
public record MediaUploadRequestDto(

        @Schema(description = "Original file name", example = "module-1-introduction.mp4")
        @NotBlank(message = "File name is required.")
        @Size(max = 255, message = "File name must not exceed 255 characters.")
        String fileName,

        @Schema(description = "MIME type", example = "video/mp4")
        @NotBlank(message = "Content type is required.")
        String contentType,

        @Schema(description = "Exact file size in bytes", example = "21474836480")
        @Positive(message = "File size must be greater than zero.")
        @Max(value = MediaUploadRequestDto.MAX_FILE_SIZE_BYTES, message = "A single file must not exceed 50 GiB.")
        long sizeBytes,

        @Schema(description = "Whether this is a video or a document")
        @NotNull(message = "Media kind is required.")
        MediaKind kind,

        @Schema(description = "Video length in seconds (videos only, max 20 hours)", example = "72000")
        @Positive(message = "Video duration must be greater than zero.")
        @Max(value = MediaUploadRequestDto.MAX_VIDEO_SECONDS, message = "A video must not exceed 20 hours.")
        Integer durationSeconds,

        @Schema(description = "Optional SHA-256 of the whole file, used to verify the upload")
        @Pattern(regexp = "^[a-fA-F0-9]{64}$", message = "Checksum must be a SHA-256 hex string.")
        String sha256
) {
    public static final long MAX_FILE_SIZE_BYTES = 53_687_091_200L;   // 50 GiB
    public static final long MAX_DOCUMENT_SIZE_BYTES = 524_288_000L;  // 500 MiB
    public static final long MAX_VIDEO_SECONDS = 72_000L;              // 20 hours

    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4", "video/webm", "video/quicktime", "video/x-matroska");

    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain");

    @AssertTrue(message = "Content type does not match the media kind.")
    public boolean isContentTypeValid() {
        if (kind == null || contentType == null) return true;
        String type = contentType.toLowerCase();
        return kind == MediaKind.VIDEO ? VIDEO_TYPES.contains(type) : DOCUMENT_TYPES.contains(type);
    }

    @AssertTrue(message = "Videos require a duration; documents must not have one.")
    public boolean isDurationValid() {
        if (kind == null) return true;
        return (kind == MediaKind.VIDEO) == (durationSeconds != null);
    }

    @AssertTrue(message = "A document must not exceed 500 MiB.")
    public boolean isDocumentSizeValid() {
        return kind != MediaKind.DOCUMENT || sizeBytes <= MAX_DOCUMENT_SIZE_BYTES;
    }
}