package com.asohCloak.asohCloak.utils.specification.userSpecification;

import com.asohCloak.asohCloak.dto.user.UserSearchRequestDto;
import com.asohCloak.asohCloak.entity.user.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> build(UserSearchRequestDto request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isFalse(root.get("accountDeleted")));

            if (request.keyword() != null && !request.keyword().isBlank()) {
                String pattern = "%" + request.keyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            if (request.role() != null) {
                predicates.add(cb.equal(root.get("role"), request.role()));
            }
            if (request.accountVerified() != null) {
                predicates.add(cb.equal(root.get("accountVerified"), request.accountVerified()));
            }
            if (request.accountBlocked() != null) {
                predicates.add(cb.equal(root.get("accountBlocked"), request.accountBlocked()));
            }
            if (request.accountSuspended() != null) {
                predicates.add(cb.equal(root.get("accountSuspended"), request.accountSuspended()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}