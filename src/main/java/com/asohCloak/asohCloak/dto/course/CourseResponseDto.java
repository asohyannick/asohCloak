package com.asohCloak.asohCloak.dto.course;

import com.asohCloak.asohCloak.enums.CourseLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Course details returned by the API")
public record CourseResponseDto(

        @Schema(description = "Course ID") UUID id,

        @Schema(description = "Course title") String name,

        @Schema(description = "URL-friendly identifier") String slug,

        @Schema(description = "Full description") String description,

        @Schema(description = "Short summary shown in listings") String shortDescription,

        @Schema(description = "Thumbnail image URL") String thumbnailUrl,

        @Schema(description = "Course price", example = "25000.00") BigDecimal price,

        @Schema(description = "ISO 4217 currency code", example = "XAF") String currency,

        @Schema(description = "Difficulty level") CourseLevel level,

        @Schema(description = "Category") String category,

        @Schema(description = "Searchable tags") List<String> tags,

        @Schema(description = "Assigned instructor") CourseInstructorSummaryDto instructor,

        @Schema(description = "Total duration in minutes") Integer durationInMinutes,

        @Schema(description = "Attached files with their upload status") List<CourseMediaDto> media,

        @Schema(description = "Brochure PDF download link, valid for 1 hour") String brochureUrl,

        @Schema(description = "Number of enrolled students") Integer enrolledCount,

        @Schema(description = "Whether the course is visible to students") Boolean published,

        @Schema(description = "When the course was published") Instant publishedAt,

        @Schema(description = "Creation time") Instant createdAt,

        @Schema(description = "Last update time") Instant updatedAt
) { }