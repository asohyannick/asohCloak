package com.asohCloak.asohCloak.dto.user;

import java.util.List;

public record PagedResponseDto<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last
) {
}