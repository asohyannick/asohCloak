package com.asohCloak.asohCloak.utils.specification.courseSpecification;

import com.asohCloak.asohCloak.dto.course.CourseSearchRequestDto;
import com.asohCloak.asohCloak.entity.course.Course;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CourseSpecification {

    public static Specification<Course> build(CourseSearchRequestDto request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (request.keyword() != null && !request.keyword().isBlank()) {
                String like = "%" + request.keyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("category")), like)
                ));
            }
            if (request.level() != null) {
                predicates.add(cb.equal(root.get("level"), request.level()));
            }
            if (request.category() != null && !request.category().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), request.category().toLowerCase()));
            }
            if (request.instructorId() != null) {
                predicates.add(cb.equal(root.get("instructor").get("id"), request.instructorId()));
            }
            if (request.published() != null) {
                predicates.add(cb.equal(root.get("published"), request.published()));
            }
            if (request.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.minPrice()));
            }
            if (request.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.maxPrice()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}