package com.asohCloak.asohCloak.dto.course;
import com.asohCloak.asohCloak.enums.CourseLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "JSON payload for creating a course. Files are NOT sent here; "
        + "each entry in 'media' describes a file the client will upload directly to storage.")
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
        @Size(max = 2048, message = "Thumbnail URL is too long.")
        String thumbnailUrl,

        @Schema(description = "Course price", example = "25000.00")
        @NotNull(message = "Price is required.")
        @DecimalMin(value = "0.00", message = "Price cannot be negative.")
        @Digits(integer = 12, fraction = 2, message = "Price must have at most 2 decimal places.")
        BigDecimal price,

        @Schema(description = "ISO 4217 currency code", example = "XAF")
        @NotBlank(message = "Currency is required.")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase ISO code.")
        String currency,

        @Schema(description = "Difficulty level")
        @NotNull(message = "Course level is required.")
        CourseLevel level,

        @Schema(description = "Course category", example = "Web Development")
        @Size(max = 100, message = "Category must not exceed 100 characters.")
        String category,

        @Schema(description = "Searchable tags")
        @Size(max = 20, message = "A course can have at most 20 tags.")
        List<@NotBlank(message = "Tags must not be blank.")
        @Size(max = 50, message = "A tag must not exceed 50 characters.") String> tags,

        @Schema(description = "ID of the user assigned as instructor")
        @NotNull(message = "Instructor is required.")
        UUID instructorId,

        @Schema(description = "Total course duration in minutes", example = "1200")
        @NotNull(message = "Duration is required.")
        @Positive(message = "Duration must be greater than zero.")
        @Max(value = 60_000, message = "Total duration must not exceed 1000 hours.")
        Integer durationInMinutes,

        @Schema(description = "Files the client will upload after the course is created")
        @Size(max = 50, message = "At most 50 files can be attached per request.")
        List<@NotNull @Valid MediaUploadRequestDto> media
) {
        public List<MediaUploadRequestDto> mediaOrEmpty() {
                return media == null ? List.of() : media;
        }
}