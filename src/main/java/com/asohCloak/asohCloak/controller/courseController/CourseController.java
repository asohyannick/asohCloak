package com.asohCloak.asohCloak.controller.courseController;

import com.asohCloak.asohCloak.config.globalSuccessResponse.GlobalSuccessResponse;
import com.asohCloak.asohCloak.dto.course.CourseRequestDto;
import com.asohCloak.asohCloak.dto.course.CourseResponseDto;
import com.asohCloak.asohCloak.dto.course.CourseSearchRequestDto;
import com.asohCloak.asohCloak.dto.user.PagedResponseDto;
import com.asohCloak.asohCloak.service.courseService.CourseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "Course Management Endpoints")
public class CourseController {

    private final CourseService courseService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalSuccessResponse<CourseResponseDto>> createCourse(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestPart("course") CourseRequestDto courseRequestDto,
            @RequestPart(value = "videos", required = false) List<MultipartFile> videos,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {
        CourseResponseDto response = courseService.createCourse(idempotencyKey, courseRequestDto, videos, documents);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalSuccessResponse<>(
                "Course created successfully. Media uploads are processing in the background.", response, 201));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalSuccessResponse<CourseResponseDto>> updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestPart("course") CourseRequestDto courseRequestDto,
            @RequestPart(value = "videos", required = false) List<MultipartFile> videos,
            @RequestPart(value = "documents", required = false) List<MultipartFile> documents) {
        CourseResponseDto response = courseService.updateCourse(courseId, courseRequestDto, videos, documents);
        return ResponseEntity.ok(new GlobalSuccessResponse<>("Course updated successfully.", response, 200));
    }

    @GetMapping
    public ResponseEntity<GlobalSuccessResponse<PagedResponseDto<CourseResponseDto>>> fetchCourses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        PagedResponseDto<CourseResponseDto> response = courseService.fetchCourses(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(new GlobalSuccessResponse<>("Courses fetched successfully.", response, 200));
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
}