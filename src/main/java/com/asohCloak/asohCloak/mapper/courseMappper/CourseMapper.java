package com.asohCloak.asohCloak.mapper.courseMappper;
import com.asohCloak.asohCloak.dto.course.*;
import com.asohCloak.asohCloak.entity.course.Course;
import com.asohCloak.asohCloak.entity.courseMedia.CourseMedia;
import com.asohCloak.asohCloak.entity.user.User;
import com.asohCloak.asohCloak.enums.MediaStatus;
import com.asohCloak.asohCloak.service.minioStorageService.MinioStorageService;
import org.mapstruct.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public abstract class CourseMapper {

    private static final Logger log = LoggerFactory.getLogger(CourseMapper.class);
    private static final Duration BROCHURE_URL_TTL = Duration.ofHours(1);

    @Autowired
    protected MinioStorageService minioStorageService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "instructor", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "uploadVideos", ignore = true)
    @Mapping(target = "uploadDocuments", ignore = true)
    @Mapping(target = "brochureObjectKey", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "enrolledCount", ignore = true)
    @Mapping(target = "published", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract Course toEntity(CourseRequestDto courseRequestDto);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            unmappedTargetPolicy = ReportingPolicy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instructor", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "uploadVideos", ignore = true)
    @Mapping(target = "uploadDocuments", ignore = true)
    @Mapping(target = "brochureObjectKey", ignore = true)
    public abstract void updateEntityFromDto(CourseUpdateRequestDto dto, @MappingTarget Course course);

    // Only brochureUrl is presigned; every other String is copied as-is.
    @Mapping(target = "brochureUrl", source = "brochureObjectKey", qualifiedByName = "presignObjectKey")
    @Mapping(target = "media", source = "media", qualifiedByName = "toMediaDtos")
    public abstract CourseResponseDto toResponseDto(Course course);

    public abstract CourseInstructorSummaryDto toInstructorSummaryDto(User user);

    /**
     * @Named keeps MapStruct from applying this to every String property.
     * Without it, MapStruct treats any String -> String method as a global conversion.
     */
    @Named("presignObjectKey")
    protected String presignObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try {
            return minioStorageService.generatePresignedGetUrl(objectKey, BROCHURE_URL_TTL);
        } catch (Exception e) {
            log.warn("Could not generate brochure URL for {}: {}", objectKey, e.getMessage());
            return null;
        }
    }

    @Named("toMediaDtos")
    protected List<CourseMediaDto> toMediaDtos(List<CourseMedia> media) {
        if (media == null) return List.of();
        return media.stream()
                .map(m -> {
                    CourseMediaDto dto = CourseMediaDto.from(m);
                    return m.getStatus() == MediaStatus.READY
                            ? new CourseMediaDto(dto.id(), dto.fileName(), dto.kind(), dto.contentType(),
                            dto.sizeBytes(), dto.durationSeconds(), dto.status(),
                            presignObjectKey(m.getObjectKey()), dto.createdAt(), dto.completedAt())
                            : dto;
                })
                .toList();
    }
}