package com.asohCloak.asohCloak.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "The updated course plus upload sessions for any newly declared files")
public record CourseUpdatedResponseDto(
        CourseResponseDto course,
        @Schema(description = "Empty when no new media was declared") List<MediaUploadSessionDto> uploads
) { }