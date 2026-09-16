package com.asohCloak.asohCloak.dto.user;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public record PageQuery(int page, int size, String sortBy, String sortDirection) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final String DEFAULT_SORT = "createdAt";
    public static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "firstName", "lastName", "email", "role", "createdAt", "updatedAt"
    );

    public PageQuery {
        page = Math.max(page, 0);
        size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        sortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy.trim())) ? sortBy.trim() : DEFAULT_SORT;
        sortDirection = "ASC".equalsIgnoreCase(sortDirection == null ? null : sortDirection.trim()) ? "ASC" : "DESC";
    }

    public static PageQuery of(Integer page, Integer size, String sortBy, String sortDirection) {
        return new PageQuery(
                page == null ? 0 : page,
                size == null ? DEFAULT_SIZE : size,
                sortBy,
                sortDirection
        );
    }

    /** Sorts by the chosen field, then by id, so rows with equal values keep a stable order across pages. */
    public Pageable toPageable() {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy)
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return PageRequest.of(page, size, sort);
    }

    public String cacheKey() {
        return "p=" + page + "|s=" + size + "|sort=" + sortBy + "|dir=" + sortDirection;
    }
}