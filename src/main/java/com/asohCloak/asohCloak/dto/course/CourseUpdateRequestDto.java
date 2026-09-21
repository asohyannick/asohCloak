package com.asohCloak.asohCloak.dto.course;

import com.asohCloak.asohCloak.enums.CourseLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CourseUpdateRequestDto(
        @Size(min = 1, max = 200) String name,
        @Size(max = 2000) String description,
        @Size(max = 500) String shortDescription,
        @Size(max = 2048) String thumbnailUrl,
        @DecimalMin("0.00") @Digits(integer = 12, fraction = 2) BigDecimal price,
        @Pattern(regexp = "^[A-Z]{3}$") String currency,
        CourseLevel level,
        @Size(max = 100) String category,
        @Size(max = 20) List<@NotBlank @Size(max = 50) String> tags,
        UUID instructorId,
        @Positive @Max(60_000) Integer durationInMinutes,
        @Size(max = 50) List<@NotNull @Valid MediaUploadRequestDto> mediaToAdd
) {
    public List<MediaUploadRequestDto> mediaToAddOrEmpty() {
        return mediaToAdd == null ? List.of() : mediaToAdd;
    }
}
