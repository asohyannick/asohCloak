package com.asohCloak.asohCloak.dto.course;

import com.asohCloak.asohCloak.enums.CourseLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

@Schema(description = "Payload for creating or updating a course (sent as the 'course' part of a multipart request)")
public record CourseRequestDto(

        @Schema(description = "Course title", example = "Introduction to Backend Development")
        @NotBlank(message = "Course name is required.")
        @Size(max = 200, message = "Course name must not exceed 200 characters.")
        String name,

        @Schema(description = "Full course description")
        @Size(max = 2000, message = "Description must not exceed 2000 characters.")
        String description,

        @Schema(description = "Short summary shown in course listings")
        @Size(max = 500, message = "Short description must not exceed 500 characters.")
        String shortDescription,

        @Schema(description = "URL of the course thumbnail image")
        String thumbnailUrl,

        @Schema(description = "Course price", example = "25000")
        @PositiveOrZero(message = "Price cannot be negative.")
        double price,

        @Schema(description = "ISO currency code", example = "XAF")
        @NotBlank(message = "Currency is required.")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code.")
        String currency,

        @Schema(description = "Difficulty level")
        @NotNull(message = "Course level is required.")
        CourseLevel level,

        @Schema(description = "Course category", example = "Web Development")
        @Size(max = 100, message = "Category must not exceed 100 characters.")
        String category,

        @Schema(description = "Searchable tags")
        List<String> tags,

        @Schema(description = "ID of the user assigned as instructor")
        @NotNull(message = "Instructor is required.")
        UUID instructorId,

        @Schema(description = "Total course duration, in minutes")
        @Positive(message = "Duration must be greater than zero.")
        int durationInMinutes
) { }