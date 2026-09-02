package com.asohCloak.asohCloak.dto.user;
import com.asohCloak.asohCloak.enums.UserRole;

public record UserSearchRequestDto(
        String keyword,
        UserRole role,
        Boolean accountVerified,
        Boolean accountBlocked,
        Boolean accountSuspended,
        Integer page,
        Integer size,
        String sortBy,
        String sortDirection
) {
    public int pageOrDefault() {
        return page == null || page < 0 ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null || size <= 0 ? 20 : Math.min(size, 100);
    }

    public String sortByOrDefault() {
        return (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
    }

    public String sortDirectionOrDefault() {
        return sortDirection == null ? "DESC" : sortDirection;
    }
}