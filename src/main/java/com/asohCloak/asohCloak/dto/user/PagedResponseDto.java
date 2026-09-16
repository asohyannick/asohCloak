package com.asohCloak.asohCloak.dto.user;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PagedResponseDto<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean hasNext,
        boolean hasPrevious
) {
    public static <E, T> PagedResponseDto<T> from(Page<E> page, Function<E, T> mapper) {
        return new PagedResponseDto<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getNumberOfElements(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}