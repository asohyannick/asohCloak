package com.asohCloak.asohCloak.dto.course;
import com.asohCloak.asohCloak.enums.CourseLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Course details returned by the API")
public record CourseResponseDto(

        @Schema(description = "Internal course ID") UUID id,
        @Schema(description = "Course title") String name,
        @Schema(description = "URL-friendly slug") String slug,
        @Schema(description = "Full course description") String description,
        @Schema(description = "Short summary") String shortDescription,
        @Schema(description = "Uploaded video URLs") List<String> uploadVideos,
        @Schema(description = "Uploaded document URLs") List<String> uploadDocuments,
        @Schema(description = "Thumbnail image URL") String thumbnailUrl,
        @Schema(description = "Course price") double price,
        @Schema(description = "ISO currency code") String currency,
        @Schema(description = "Difficulty level") CourseLevel level,
        @Schema(description = "Course category") String category,
        @Schema(description = "Searchable tags") List<String> tags,
        @Schema(description = "Assigned instructor") CourseInstructorSummaryDto instructor,
        @Schema(description = "Total duration, in minutes") int durationInMinutes,
        @Schema(description = "Number of enrolled students") int enrolledCount,
        @Schema(description = "Whether the course is publicly published") boolean published,
        @Schema(description = "When the course was published") Instant publishedAt,
        @Schema(description = "When the course was created") Instant createdAt,
        @Schema(description = "When the course was last updated") Instant updatedAt
) { }