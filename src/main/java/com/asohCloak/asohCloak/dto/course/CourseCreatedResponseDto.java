package com.asohCloak.asohCloak.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "The newly created course plus one upload session per declared file")
public record CourseCreatedResponseDto(

        @Schema(description = "The created course") CourseResponseDto course,

        @Schema(description = "Presigned upload sessions; empty when no media was declared")
        List<MediaUploadSessionDto> uploads
) { }