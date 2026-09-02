package com.asohCloak.asohCloak.service.courseService;

import com.asohCloak.asohCloak.config.asyncScheduler.asyncTaskRunner.AsyncTaskRunner;
import com.asohCloak.asohCloak.config.emailTemplateMessager.EmailTemplateMessager;
import com.asohCloak.asohCloak.dto.course.CourseRequestDto;
import com.asohCloak.asohCloak.dto.course.CourseResponseDto;
import com.asohCloak.asohCloak.dto.course.CourseSearchRequestDto;
import com.asohCloak.asohCloak.dto.user.PagedResponseDto;
import com.asohCloak.asohCloak.entity.course.Course;
import com.asohCloak.asohCloak.entity.user.User;
import com.asohCloak.asohCloak.exception.badRequestException.BadRequestException;
import com.asohCloak.asohCloak.exception.conflictRequestException.ConflictRequestException;
import com.asohCloak.asohCloak.exception.notFoundRequestException.NotFoundRequestException;
import com.asohCloak.asohCloak.mapper.courseMappper.CourseMapper;
import com.asohCloak.asohCloak.repository.courseRepository.CourseRepository;
import com.asohCloak.asohCloak.repository.userRepository.UserRepository;
import com.asohCloak.asohCloak.service.minioStorageService.MinioStorageService;
import com.asohCloak.asohCloak.service.resendMailService.ResendMailService;
import com.asohCloak.asohCloak.utils.specification.courseSpecification.CourseSpecification;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "price", "createdAt", "updatedAt", "enrolledCount");
    private static final Duration PRESIGNED_URL_TTL = Duration.ofDays(7);
    private static final Duration MEDIA_UPLOAD_TIMEOUT = Duration.ofMinutes(15);
    private static final Duration MEDIA_NOTIFY_DELAY = Duration.ofSeconds(5);

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseMapper courseMapper;
    private final AsyncTaskRunner asyncTaskRunner;
    private final ResendMailService resendMailService;
    private final MinioStorageService minioStorageService;
    private final CacheManager cacheManager;

    @Value("${app.frontend.course-url}")
    private String frontendCourseUrl;

    // =====================================================================
    // 1. CREATE
    // =====================================================================
    @CacheEvict(cacheNames = {"courses", "courseSearch", "courseCount"}, allEntries = true)
    public CourseResponseDto createCourse(String idempotencyKey, CourseRequestDto courseRequestDto,
                                          List<MultipartFile> videos, List<MultipartFile> documents) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required.");
        }
        if (courseRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            throw new ConflictRequestException("A course was already created with this idempotency key.");
        }

        User instructor = userRepository.findById(courseRequestDto.instructorId())
                .orElseThrow(() -> new NotFoundRequestException("No instructor found with this id."));

        // Copy multipart content to durable temp files now — the request's own
        // multipart temp files are gone once this method returns.
        List<StagedFile> stagedVideos = stageFiles(videos);
        List<StagedFile> stagedDocuments = stageFiles(documents);

        Course course = courseMapper.toEntity(courseRequestDto);
        course.setInstructor(instructor);
        course.setSlug(buildSlug(courseRequestDto.name()));
        course.setIdempotencyKey(idempotencyKey);
        course.setUploadVideos(new ArrayList<>());
        course.setUploadDocuments(new ArrayList<>());

        Course savedCourse;
        try {
            savedCourse = courseRepository.save(course);
        } catch (DataIntegrityViolationException e) {
            // A concurrent request with the same key won the race at the DB level.
            cleanupStagedFiles(stagedVideos, stagedDocuments);
            throw new ConflictRequestException("A course was already created with this idempotency key.");
        }

        String courseUrl = frontendCourseUrl + "/" + savedCourse.getSlug();

        sendCourseCreatedEmail(instructor, savedCourse.getName(), courseUrl);

        if (!stagedVideos.isEmpty() || !stagedDocuments.isEmpty()) {
            queueMediaProcessing(savedCourse.getId(), stagedVideos, stagedDocuments,
                    instructor, savedCourse.getName(), courseUrl);
        }

        return courseMapper.toResponseDto(savedCourse);
    }

    // =====================================================================
    // 2. UPDATE
    // =====================================================================
    @Caching(evict = {
            @CacheEvict(cacheNames = "course", key = "#courseId"),
            @CacheEvict(cacheNames = {"courses", "courseSearch"}, allEntries = true)
    })
    public CourseResponseDto updateCourse(UUID courseId, CourseRequestDto courseRequestDto,
                                          List<MultipartFile> newVideos, List<MultipartFile> newDocuments) {

        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new NotFoundRequestException("No course found with this id."));

        UUID currentInstructorId = course.getInstructor() != null ? course.getInstructor().getId() : null;
        if (courseRequestDto.instructorId() != null && !courseRequestDto.instructorId().equals(currentInstructorId)) {
            User newInstructor = userRepository.findById(courseRequestDto.instructorId())
                    .orElseThrow(() -> new NotFoundRequestException("No instructor found with this id."));
            course.setInstructor(newInstructor);
        }

        courseMapper.updateEntityFromDto(courseRequestDto, course);
        if (courseRequestDto.name() != null) {
            course.setSlug(buildSlug(courseRequestDto.name()));
        }

        List<StagedFile> stagedVideos = stageFiles(newVideos);
        List<StagedFile> stagedDocuments = stageFiles(newDocuments);

        Course savedCourse = courseRepository.save(course);

        if (!stagedVideos.isEmpty() || !stagedDocuments.isEmpty()) {
            String courseUrl = frontendCourseUrl + "/" + savedCourse.getSlug();
            queueMediaProcessing(savedCourse.getId(), stagedVideos, stagedDocuments,
                    savedCourse.getInstructor(), savedCourse.getName(), courseUrl);
        }

        return courseMapper.toResponseDto(savedCourse);
    }

    // =====================================================================
    // 3. FETCH (paginated list) — cached
    // =====================================================================
    @Cacheable(cacheNames = "courses",
            key = "'page_' + #page + '_size_' + #size + '_sort_' + #sortBy + '_' + #sortDirection")
    public PagedResponseDto<CourseResponseDto> fetchCourses(int page, int size, String sortBy, String sortDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, resolveSort(sortBy, sortDirection));
        Page<Course> coursePage = courseRepository.findByDeletedFalse(pageable);
        return toPagedResponse(coursePage);
    }

    // =====================================================================
    // 4. FETCH (single) — cached
    // =====================================================================
    @Cacheable(cacheNames = "course", key = "#courseId")
    public CourseResponseDto fetchCourse(UUID courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new NotFoundRequestException("No course found with this id."));
        return courseMapper.toResponseDto(course);
    }

    // =====================================================================
    // 5. DELETE (soft delete)
    // =====================================================================
    @Caching(evict = {
            @CacheEvict(cacheNames = "course", key = "#courseId"),
            @CacheEvict(cacheNames = {"courses", "courseSearch", "courseCount"}, allEntries = true)
    })
    public void deleteCourse(UUID courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new NotFoundRequestException("No course found with this id."));
        course.setDeleted(true);
        courseRepository.save(course);
    }

    // =====================================================================
    // 6. SEARCH (filter + sort + paginate) — cached
    // =====================================================================
    @Cacheable(
            cacheNames = "courseSearch",
            key = "T(java.util.Objects).hash(#request.keyword(), #request.level(), #request.category(), " +
                    "#request.instructorId(), #request.published(), #request.minPrice(), #request.maxPrice(), " +
                    "#request.pageOrDefault(), #request.sizeOrDefault(), #request.sortByOrDefault(), #request.sortDirectionOrDefault())"
    )
    public PagedResponseDto<CourseResponseDto> searchCourses(CourseSearchRequestDto request) {
        Pageable pageable = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                resolveSort(request.sortByOrDefault(), request.sortDirectionOrDefault())
        );
        Specification<Course> spec = CourseSpecification.build(request);
        Page<Course> coursePage = courseRepository.findAll(spec, pageable);
        return toPagedResponse(coursePage);
    }

    // =====================================================================
    // 7. COUNT — cached
    // =====================================================================
    @Cacheable(cacheNames = "courseCount")
    public long countCourses() {
        return courseRepository.countByDeletedFalse();
    }

    // =====================================================================
    // MEDIA PROCESSING (background)
    // =====================================================================

    private void queueMediaProcessing(UUID courseId, List<StagedFile> videos, List<StagedFile> documents,
                                      User instructor, String courseName, String courseUrl) {
        asyncTaskRunner.runInBackground(
                () -> processMedia(courseId, videos, documents),
                MEDIA_UPLOAD_TIMEOUT,
                MEDIA_NOTIFY_DELAY,
                (MediaProcessingResult result) -> {
                    appendMediaUrls(courseId, result);
                    sendMediaCompletionEmail(instructor, courseName, courseUrl, result.failedFileNames());
                },
                (Throwable ex) -> {
                    log.error("Media processing failed entirely for course {}: {}", courseId, ex.getMessage(), ex);
                    sendMediaFailureEmail(instructor, courseName, courseUrl);
                }
        );
    }

    private MediaProcessingResult processMedia(UUID courseId, List<StagedFile> videos, List<StagedFile> documents) {
        List<String> videoUrls = new ArrayList<>();
        List<String> documentUrls = new ArrayList<>();
        List<String> failedFileNames = new ArrayList<>();

        for (StagedFile file : videos) {
            uploadOne(courseId, "videos", file, videoUrls, failedFileNames);
        }
        for (StagedFile file : documents) {
            uploadOne(courseId, "documents", file, documentUrls, failedFileNames);
        }
        return new MediaProcessingResult(videoUrls, documentUrls, failedFileNames);
    }

    private void uploadOne(UUID courseId, String folder, StagedFile file, List<String> successUrls, List<String> failedNames) {
        try {
            String objectKey = "courses/%s/%s/%s-%s".formatted(
                    courseId, folder, UUID.randomUUID(), sanitizeFilename(file.originalFilename()));
            minioStorageService.uploadObject(objectKey, file.tempPath(), file.contentType());
            successUrls.add(minioStorageService.generatePresignedGetUrl(objectKey, PRESIGNED_URL_TTL));
        } catch (Exception e) {
            log.error("Failed to upload '{}' for course {}: {}", file.originalFilename(), courseId, e.getMessage(), e);
            failedNames.add(file.originalFilename());
        } finally {
            deleteQuietly(file.tempPath());
        }
    }

    /**
     * Called from the background callback (self-invocation), so @CacheEvict on
     * this method would silently do nothing — the Spring AOP proxy is bypassed
     * on internal calls. Evicting via CacheManager directly works regardless.
     */
    private void appendMediaUrls(UUID courseId, MediaProcessingResult result) {
        courseRepository.findById(courseId).ifPresent(course -> {
            if (course.getUploadVideos() == null) course.setUploadVideos(new ArrayList<>());
            if (course.getUploadDocuments() == null) course.setUploadDocuments(new ArrayList<>());
            course.getUploadVideos().addAll(result.videoUrls());
            course.getUploadDocuments().addAll(result.documentUrls());
            courseRepository.save(course);
        });

        evictIfPresent("course", courseId);
        clearIfPresent("courses");
        clearIfPresent("courseSearch");
    }

    private void sendMediaCompletionEmail(User instructor, String courseName, String courseUrl, List<String> failedFileNames) {
        asyncTaskRunner.runInBackground(
                () -> {
                    String html = failedFileNames.isEmpty()
                            ? EmailTemplateMessager.courseMediaReadyEmailAsync(
                            instructor.getFirstName(), instructor.getLastName(), courseName, courseUrl)
                            : EmailTemplateMessager.courseMediaPartiallyFailedEmailAsync(
                            instructor.getFirstName(), instructor.getLastName(), courseName, courseUrl, failedFileNames);
                    String subject = failedFileNames.isEmpty()
                            ? "Your course media is ready - AsohClock"
                            : "Some course media failed to upload - AsohClock";
                    return resendMailService.sendEmail(instructor.getEmail(), subject, html);
                },
                (CreateEmailResponse response) -> log.info("Media-processing email sent to {}", instructor.getEmail()),
                (Throwable ex) -> log.error("Failed to send media-processing email to {}: {}", instructor.getEmail(), ex.getMessage(), ex)
        );
    }

    private void sendMediaFailureEmail(User instructor, String courseName, String courseUrl) {
        asyncTaskRunner.runInBackground(
                () -> {
                    String html = EmailTemplateMessager.courseMediaPartiallyFailedEmailAsync(
                            instructor.getFirstName(), instructor.getLastName(), courseName, courseUrl,
                            List.of("all uploaded files"));
                    return resendMailService.sendEmail(instructor.getEmail(), "Course media processing failed - AsohClock", html);
                },
                (CreateEmailResponse response) -> log.info("Media-failure email sent to {}", instructor.getEmail()),
                (Throwable ex) -> log.error("Failed to send media-failure email to {}: {}", instructor.getEmail(), ex.getMessage(), ex)
        );
    }

    private void sendCourseCreatedEmail(User instructor, String courseName, String courseUrl) {
        asyncTaskRunner.runInBackground(
                () -> {
                    String html = EmailTemplateMessager.sentCourseEmailAsync(
                            instructor.getFirstName(), instructor.getLastName(), courseName, courseUrl);
                    return resendMailService.sendEmail(instructor.getEmail(), "Course created - AsohClock", html);
                },
                (CreateEmailResponse response) -> log.info("Course-created email sent to {}", instructor.getEmail()),
                (Throwable ex) -> log.error("Failed to send course-created email to {}: {}", instructor.getEmail(), ex.getMessage(), ex)
        );
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private Sort resolveSort(String sortBy, String sortDirection) {
        String field = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private PagedResponseDto<CourseResponseDto> toPagedResponse(Page<Course> coursePage) {
        List<CourseResponseDto> content = coursePage.getContent().stream()
                .map(courseMapper::toResponseDto)
                .toList();
        return new PagedResponseDto<>(
                content, coursePage.getNumber(), coursePage.getSize(),
                coursePage.getTotalElements(), coursePage.getTotalPages(), coursePage.isLast()
        );
    }

    private String buildSlug(String name) {
        String base = name.toLowerCase().trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
        String candidate = base;
        int suffix = 1;
        while (courseRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private List<StagedFile> stageFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return List.of();
        List<StagedFile> staged = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            try {
                Path tempFile = Files.createTempFile("course-media-", "-" + sanitizeFilename(file.getOriginalFilename()));
                file.transferTo(tempFile);
                staged.add(new StagedFile(tempFile, file.getOriginalFilename(), file.getContentType()));
            } catch (IOException e) {
                log.error("Failed to stage uploaded file '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
                throw new BadRequestException("Failed to read uploaded file: " + file.getOriginalFilename());
            }
        }
        return staged;
    }

    private void cleanupStagedFiles(List<StagedFile> a, List<StagedFile> b) {
        a.forEach(f -> deleteQuietly(f.tempPath()));
        b.forEach(f -> deleteQuietly(f.tempPath()));
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temp file {}: {}", path, e.getMessage());
        }
    }

    private void evictIfPresent(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.evict(key);
    }

    private void clearIfPresent(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
    }

    private record StagedFile(Path tempPath, String originalFilename, String contentType) { }

    private record MediaProcessingResult(List<String> videoUrls, List<String> documentUrls, List<String> failedFileNames) { }
}