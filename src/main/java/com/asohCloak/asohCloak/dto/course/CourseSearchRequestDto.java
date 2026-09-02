package com.asohCloak.asohCloak.dto.course;

import com.asohCloak.asohCloak.enums.CourseLevel;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Filter and pagination criteria for searching courses")
public record CourseSearchRequestDto(

        @Schema(description = "Free-text search across name, description, and category")
        String keyword,

        @Schema(description = "Filter by difficulty level")
        CourseLevel level,

        @Schema(description = "Filter by category")
        String category,

        @Schema(description = "Filter by instructor ID")
        UUID instructorId,

        @Schema(description = "Filter by published status")
        Boolean published,

        @Schema(description = "Minimum price")
        Double minPrice,

        @Schema(description = "Maximum price")
        Double maxPrice,

        @Schema(description = "Page number, zero-based") Integer page,
        @Schema(description = "Page size") Integer size,
        @Schema(description = "Field to sort by") String sortBy,
        @Schema(description = "Sort direction: ASC or DESC") String sortDirection
) {
    public int pageOrDefault() {
        return (page != null && page >= 0) ? page : 0;
    }

    public int sizeOrDefault() {
        return (size != null && size > 0) ? Math.min(size, 100) : 20;
    }

    public String sortByOrDefault() {
        return sortBy != null ? sortBy : "createdAt";
    }

    public String sortDirectionOrDefault() {
        return sortDirection != null ? sortDirection : "DESC";
    }
}