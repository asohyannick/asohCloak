package com.asohCloak.asohCloak.config.rateLimitConfig.rateLimitKeyResolver;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * Resolves the Bucket4j cache key for a request and tracks the client IP.
 * Authenticated requests are keyed by the Keycloak subject (JWT "sub" claim),
 * so a user is rate-limited consistently regardless of device or network.
 * Unauthenticated requests (e.g. /auth/login, /auth/register) fall back to IP,
 * so brute-force attempts against public endpoints are still capped.
 */
@Service("rateLimitKeyResolver")
public class RateLimitKeyResolver {

    private static final Logger log = LoggerFactory.getLogger(RateLimitKeyResolver.class);

    public String resolveKey(HttpServletRequest request) {
        String ip = getClientIp(request);
        String userId = resolveAuthenticatedUserId();

        String key = (userId != null) ? "user:" + userId : "ip:" + ip;

        log.debug("Rate limit key resolved - key={}, ip={}, uri={}", key, ip, request.getRequestURI());

        return key;
    }

    public String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String resolveAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return jwt.getSubject();
        }
        return null;
    }
}