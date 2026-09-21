package com.asohCloak.asohCloak.dto.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record CompleteMediaUploadRequestDto(
        @NotBlank String uploadId,
        @NotEmpty @Size(max = 10_000)
        List<@NotNull @Valid CompletedPartDto> parts
) { }