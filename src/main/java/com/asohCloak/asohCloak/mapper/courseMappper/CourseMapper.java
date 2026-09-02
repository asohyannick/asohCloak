package com.asohCloak.asohCloak.mapper.courseMappper;

import com.asohCloak.asohCloak.dto.course.CourseInstructorSummaryDto;
import com.asohCloak.asohCloak.dto.course.CourseRequestDto;
import com.asohCloak.asohCloak.dto.course.CourseResponseDto;
import com.asohCloak.asohCloak.entity.course.Course;
import com.asohCloak.asohCloak.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CourseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "instructor", ignore = true)
    @Mapping(target = "enrolledCount", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Course toEntity(CourseRequestDto courseRequestDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "instructor", ignore = true)
    @Mapping(target = "enrolledCount", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(CourseRequestDto courseRequestDto, @MappingTarget Course course);

    CourseResponseDto toResponseDto(Course course);

    CourseInstructorSummaryDto toInstructorSummaryDto(User user);
}