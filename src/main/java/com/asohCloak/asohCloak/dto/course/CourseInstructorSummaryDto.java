package com.asohCloak.asohCloak.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Minimal instructor info attached to a course response")
public record CourseInstructorSummaryDto(
        @Schema(description = "Instructor's internal user ID") UUID id,
        @Schema(description = "Instructor's first name") String firstName,
        @Schema(description = "Instructor's last name") String lastName
) { }