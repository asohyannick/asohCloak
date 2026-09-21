package com.asohCloak.asohCloak.service.courseMediaService;

import com.asohCloak.asohCloak.dto.course.*;
import com.asohCloak.asohCloak.entity.course.Course;
import com.asohCloak.asohCloak.entity.courseMedia.CourseMedia;
import com.asohCloak.asohCloak.enums.MediaKind;
import com.asohCloak.asohCloak.enums.MediaStatus;
import com.asohCloak.asohCloak.exception.badRequestException.BadRequestException;
import com.asohCloak.asohCloak.exception.notFoundRequestException.NotFoundRequestException;
import com.asohCloak.asohCloak.repository.courseMediaRepository.CourseMediaRepository;
import com.asohCloak.asohCloak.repository.courseRepository.CourseRepository;
import com.asohCloak.asohCloak.service.multipartUploadService.MultipartUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CourseMediaService {

    private static final long PART_SIZE = 64L * 1024 * 1024;  // 64 MiB
    private static final int MAX_PARTS = 10_000;              // S3 limit -> 640 GiB max file
    private static final Duration PART_URL_TTL = Duration.ofHours(6);
    private static final int MAX_URL_BATCH = 200;

    private final CourseRepository courseRepository;
    private final CourseMediaRepository courseMediaRepository;
    private final MultipartUploadService multipartUploadService;

    /** Registers each declared file and opens a multipart upload for it. */
    public List<MediaUploadSessionDto> openUploadSessions(UUID courseId, List<MediaUploadRequestDto> files) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new NotFoundRequestException("No course found with this id."));

        List<MediaUploadSessionDto> sessions = new ArrayList<>();
        for (MediaUploadRequestDto file : files) {
            int partCount = (int) Math.ceil((double) file.sizeBytes() / PART_SIZE);
            if (partCount > MAX_PARTS) {
                throw new BadRequestException("File '" + file.fileName() + "' is too large to upload.");
            }

            String folder = file.kind() == MediaKind.VIDEO ? "videos" : "documents";
            String objectKey = "courses/%s/%s/%s-%s".formatted(
                    courseId,
                    folder,
                    UUID.randomUUID(),
                    sanitize(file.fileName())
            );

            String uploadId = multipartUploadService.initiate(objectKey, file.contentType());

            CourseMedia media = new CourseMedia();
            media.setCourse(course);
            media.setKind(file.kind());
            media.setFileName(file.fileName());
            media.setContentType(file.contentType());
            media.setSizeBytes(file.sizeBytes());
            media.setDurationSeconds(file.durationSeconds());
            media.setSha256(file.sha256());
            media.setObjectKey(objectKey);
            media.setUploadId(uploadId);
            media.setPartSizeBytes(PART_SIZE);
            media.setPartCount(partCount);
            media.setStatus(MediaStatus.UPLOADING);
            courseMediaRepository.saveAndFlush(media);

            sessions.add(toSession(media, presignRange(media, 1, Math.min(partCount, MAX_URL_BATCH))));
        }
        return sessions;
    }

    /** Fresh URLs for a batch of parts; long uploads outlive the first batch. */
    @Transactional(readOnly = true)
    public MediaUploadSessionDto partUrls(UUID courseId, UUID mediaId, int from, int count) {
        CourseMedia media = require(courseId, mediaId);
        if (media.getStatus() != MediaStatus.UPLOADING) {
            throw new BadRequestException("This file is no longer accepting uploads.");
        }
        int first = Math.max(from, 1);
        int last = Math.min(first + Math.min(count, MAX_URL_BATCH) - 1, media.getPartCount());
        return toSession(media, presignRange(media, first, last));
    }

    /** Finalises the file in storage once every chunk has been uploaded. */
    public CourseMediaDto complete(UUID courseId, UUID mediaId, CompleteMediaUploadRequestDto request) {
        CourseMedia media = require(courseId, mediaId);
        if (media.getStatus() == MediaStatus.READY) {
            return CourseMediaDto.from(media);
        }
        if (media.getStatus() != MediaStatus.UPLOADING) {
            throw new BadRequestException("This upload was aborted; start a new one.");
        }
        if (!request.uploadId().equals(media.getUploadId())) {
            throw new BadRequestException("Upload id does not match this file.");
        }
        if (request.parts().size() != media.getPartCount()) {
            throw new BadRequestException("Expected %d parts but received %d."
                    .formatted( media.getFileName(), media.getPartCount(), request.parts().size()));
        }

        List<CompletedPart> parts = request.parts().stream()
                .sorted(Comparator.comparingInt(CompletedPartDto::partNumber))
                .map(p -> CompletedPart.builder().partNumber(p.partNumber()).eTag(p.eTag()).build())
                .toList();

        try {
            multipartUploadService.complete(media.getObjectKey(), media.getUploadId(), parts);
        } catch (RuntimeException e) {
            media.setStatus(MediaStatus.FAILED);
            log.error("Failed to complete upload {} for course {}: {}", mediaId, courseId, e.getMessage(), e);
            throw new BadRequestException("Storage rejected the upload. Please retry the file.");
        }

        media.setStatus(MediaStatus.READY);
        media.setCompletedAt(Instant.now());
        media.setUploadId(null);
        return CourseMediaDto.from(courseMediaRepository.saveAndFlush(media));
    }

    public void abort(UUID courseId, UUID mediaId) {
        CourseMedia media = require(courseId, mediaId);
        abortQuietly(media);
        courseMediaRepository.delete(media);
    }

    @Transactional(readOnly = true)
    public List<CourseMediaDto> list(UUID courseId) {
        return courseMediaRepository.findByCourseIdOrderByCreatedAtAsc(courseId)
                .stream().map(CourseMediaDto::from).toList();
    }

    /** Fresh part URLs for every file still uploading; used when a create request is replayed. */
    @Transactional(readOnly = true)
    public List<MediaUploadSessionDto> resumeUploadSessions(UUID courseId) {
        return courseMediaRepository.findByCourseIdOrderByCreatedAtAsc(courseId).stream()
                .filter(media -> media.getStatus() == MediaStatus.UPLOADING)
                .map(media -> toSession(media,
                        presignRange(media, 1, Math.min(media.getPartCount(), MAX_URL_BATCH))))
                .toList();
    }

    /** Releases storage held by uploads that were never finished. */
    public int abortStaleUploads(Duration olderThan) {
        List<CourseMedia> stale = courseMediaRepository.findByStatusAndCreatedAtBefore(
                MediaStatus.UPLOADING, Instant.now().minus(olderThan));
        stale.forEach(media -> {
            abortQuietly(media);
            media.setStatus(MediaStatus.ABORTED);
            media.setUploadId(null);
        });
        return stale.size();
    }

    private void abortQuietly(CourseMedia media) {
        if (media.getUploadId() == null) return;
        try {
            multipartUploadService.abort(media.getObjectKey(), media.getUploadId());
        } catch (RuntimeException e) {
            log.warn("Could not abort upload {}: {}", media.getId(), e.getMessage());
        }
    }

    private CourseMedia require(UUID courseId, UUID mediaId) {
        return courseMediaRepository.findByIdAndCourseId(mediaId, courseId)
                .orElseThrow(() -> new NotFoundRequestException("No media found for this course."));
    }

    private List<PresignedPartDto> presignRange(CourseMedia media, int first, int last) {
        List<PresignedPartDto> urls = new ArrayList<>();
        for (int n = first; n <= last; n++) {
            urls.add(new PresignedPartDto(n,
                    multipartUploadService.presignPart(
                            media.getObjectKey(), media.getUploadId(), n, PART_URL_TTL)));
        }
        return urls;
    }

    private MediaUploadSessionDto toSession(CourseMedia media, List<PresignedPartDto> parts) {
        return new MediaUploadSessionDto(
                media.getId(), media.getFileName(), media.getKind(), media.getUploadId(),
                media.getPartSizeBytes(), media.getPartCount(), parts,
                Instant.now().plus(PART_URL_TTL));
    }

    private String sanitize(String filename) {
        return filename == null || filename.isBlank()
                ? "file" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
