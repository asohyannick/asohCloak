package com.asohCloak.asohCloak.repository.courseMediaRepository;

import com.asohCloak.asohCloak.entity.courseMedia.CourseMedia;
import com.asohCloak.asohCloak.enums.MediaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseMediaRepository extends JpaRepository<CourseMedia, UUID> {
    List<CourseMedia> findByCourseIdOrderByCreatedAtAsc(UUID courseId);
    Optional<CourseMedia> findByIdAndCourseId(UUID id, UUID courseId);
    List<CourseMedia> findByStatusAndCreatedAtBefore(MediaStatus status, Instant cutoff);
}