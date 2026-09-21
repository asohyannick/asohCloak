package com.asohCloak.asohCloak.dto.course;

import jakarta.validation.constraints.*;

public record CompletedPartDto(
        @Min(1) @Max(10_000) int partNumber,
        @NotBlank String eTag
) { }