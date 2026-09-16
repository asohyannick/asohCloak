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
    public PageQuery toPageQuery() {
        return PageQuery.of(page, size, sortBy, sortDirection);
    }

    public int pageOrDefault()             { return toPageQuery().page(); }
    public int sizeOrDefault()             { return toPageQuery().size(); }
    public String sortByOrDefault()        { return toPageQuery().sortBy(); }
    public String sortDirectionOrDefault() { return toPageQuery().sortDirection(); }

    public String normalizedKeyword() {
        return (keyword == null || keyword.isBlank()) ? "" : keyword.trim().toLowerCase();
    }

    public String cacheKey() {
        return "kw=" + normalizedKeyword()
                + "|role=" + role
                + "|verified=" + accountVerified
                + "|blocked=" + accountBlocked
                + "|suspended=" + accountSuspended
                + "|" + toPageQuery().cacheKey();
    }
}