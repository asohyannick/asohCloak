package com.asohCloak.asohCloak.repository.courseRepository;

import com.asohCloak.asohCloak.entity.course.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

    Optional<Course> findByIdempotencyKey(String idempotencyKey);

    Optional<Course> findByIdAndDeletedFalse(UUID id);

    Page<Course> findByDeletedFalse(Pageable pageable);

    boolean existsBySlug(String slug);

    long countByDeletedFalse();
}