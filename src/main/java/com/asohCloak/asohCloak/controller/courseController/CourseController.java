package com.asohCloak.asohCloak.controller.courseController;
import com.asohCloak.asohCloak.config.globalSuccessResponse.GlobalSuccessResponse;
import com.asohCloak.asohCloak.dto.course.*;
import com.asohCloak.asohCloak.dto.user.PagedResponseDto;
import com.asohCloak.asohCloak.service.courseMediaService.CourseMediaService;
import com.asohCloak.asohCloak.service.courseService.CourseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Course Management Endpoints")
public class CourseController {

    private final CourseService courseService;
    private final CourseMediaService  courseMediaService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GlobalSuccessResponse<CourseCreatedResponseDto>> createCourse(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CourseRequestDto courseRequestDto) {
        CourseCreatedResponseDto response = courseService.createCourse(idempotencyKey, courseRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalSuccessResponse<>(
                "Course created successfully.",
                response,
                201
        ));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping(value = "/{courseId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GlobalSuccessResponse<CourseUpdatedResponseDto>> updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseUpdateRequestDto courseUpdateRequestDto) {
        CourseUpdatedResponseDto response = courseService.updateCourse(courseId, courseUpdateRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Course updated successfully.",
                response,
                200));
    }

    @GetMapping
    public ResponseEntity<GlobalSuccessResponse<PagedResponseDto<CourseResponseDto>>> fetchCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        PagedResponseDto<CourseResponseDto> response = courseService.fetchCourses(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Courses fetched successfully.",
                response,
                200));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<GlobalSuccessResponse<CourseResponseDto>> fetchCourse(@PathVariable UUID courseId) {
        CourseResponseDto response = courseService.fetchCourse(courseId);
        return ResponseEntity.ok(new GlobalSuccessResponse<>("Course fetched successfully.", response, 200));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<GlobalSuccessResponse<Void>> deleteCourse(@PathVariable UUID courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok(new GlobalSuccessResponse<>("Course deleted successfully.", null, 200));
    }

    @PostMapping("/search")
    public ResponseEntity<GlobalSuccessResponse<PagedResponseDto<CourseResponseDto>>> searchCourses(
            @RequestParam CourseSearchRequestDto request) {
        PagedResponseDto<CourseResponseDto> response = courseService.searchCourses(request);
        return ResponseEntity.ok(new GlobalSuccessResponse<>("Courses fetched successfully.", response, 200));
    }

    @GetMapping("/count")
    public ResponseEntity<GlobalSuccessResponse<Long>> countCourses() {
        return ResponseEntity.ok(new GlobalSuccessResponse<>("Course count fetched successfully.", courseService.countCourses(), 200));
    }

    @GetMapping("/{courseId}/download")
    public ResponseEntity<byte[]> downloadCourseBrochure(
            @PathVariable UUID courseId
    ) {
        byte[] pdf = courseService.generateCourseBrochure(courseId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"course-" + courseId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{courseId}/brochure")
    public ResponseEntity<GlobalSuccessResponse<Void>> regenerateBrochure(@PathVariable UUID courseId) {
        courseService.regenerateBrochure(courseId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new GlobalSuccessResponse<>(
                "Brochure generation has been queued.", null, 202));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{courseId}/media")
    public ResponseEntity<GlobalSuccessResponse<List<MediaUploadSessionDto>>> openUploads(
            @PathVariable UUID courseId,
            @Valid @RequestBody @Size(min = 1, max = 50)
            List<@Valid MediaUploadRequestDto> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalSuccessResponse<>(
                "Upload sessions created.",
                courseMediaService.openUploadSessions(courseId, files), 201));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{courseId}/media/{mediaId}/parts")
    public ResponseEntity<GlobalSuccessResponse<MediaUploadSessionDto>> partUrls(
            @PathVariable UUID courseId, @PathVariable UUID mediaId,
            @RequestParam(defaultValue = "1") int from,
            @RequestParam(defaultValue = "100") int count) {
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Part URLs issued.",
                courseMediaService.partUrls(courseId, mediaId, from, count), 200));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{courseId}/media/{mediaId}/complete")
    public ResponseEntity<GlobalSuccessResponse<CourseMediaDto>> completeUpload(
            @PathVariable UUID courseId, @PathVariable UUID mediaId,
            @Valid @RequestBody CompleteMediaUploadRequestDto request) {
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "File uploaded successfully.",
                courseMediaService.complete(courseId, mediaId, request), 200));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{courseId}/media/{mediaId}")
    public ResponseEntity<GlobalSuccessResponse<Void>> abortUpload(
            @PathVariable UUID courseId, @PathVariable UUID mediaId) {
        courseMediaService.abort(courseId, mediaId);
        return ResponseEntity.ok(new GlobalSuccessResponse<>("Upload removed.", null, 200));
    }

    @GetMapping("/{courseId}/media")
    public ResponseEntity<GlobalSuccessResponse<List<CourseMediaDto>>> listMedia(
            @PathVariable UUID courseId) {
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Media fetched successfully.",
                courseMediaService.list(courseId), 200));
    }

}