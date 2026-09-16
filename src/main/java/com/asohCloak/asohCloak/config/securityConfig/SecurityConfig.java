package com.asohCloak.asohCloak.config.securityConfig;

import com.asohCloak.asohCloak.config.securityConfig.keycloakRealmRoleConverter.KeycloakRealmRoleConverter;
import com.asohCloak.asohCloak.config.securityConfig.restAccessDeniedHandler.RestAccessDeniedHandler;
import com.asohCloak.asohCloak.config.securityConfig.restAuthenticationEntryPoint.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Security is delegated entirely to Keycloak. This app is an OAuth2 Resource
 * Server only: it never issues, stores, or validates account passwords itself.
 * The PasswordEncoder bean below exists solely for hashing OTP codes and
 * magic-link/reset tokens before they're persisted, not account credentials.
 *
 * Public endpoints are declared once in {@link #PUBLIC_ENDPOINTS} and used both
 * for permitAll() and by {@link #bearerTokenResolver()}, which ignores any
 * Authorization header on those paths. This way an expired access token sent
 * by a client can never block login, refresh-token, logout, etc.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Paths are relative to the servlet context path (/api/v1). */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/users/register",
            "/users/login",
            "/users/forgot-password",
            "/users/reset-password",
            "/users/verify-otp",
            "/users/resend-otp",
            "/users/logout",
            "/users/refresh-token",
            "/users/send-magic-link",
            "/users/verify-magic-link",
            "/users/google-login",
            "/error"
    };

    private static final String[] DOCS_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    private final CorsConfigurationSource corsConfigurationSource;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }

    /**
     * Returns no token for public and docs endpoints, so the bearer-token filter
     * skips validation there. Everywhere else, the standard Authorization header
     * resolution applies.
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver delegate = new DefaultBearerTokenResolver();
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();

        List<RequestMatcher> matchers = Stream
                .concat(Arrays.stream(PUBLIC_ENDPOINTS), Arrays.stream(DOCS_ENDPOINTS))
                .map(pattern -> (RequestMatcher) paths.matcher(pattern))
                .toList();
        RequestMatcher publicMatcher = new OrRequestMatcher(matchers);

        return request -> publicMatcher.matches(request) ? null : delegate.resolve(request);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtDecoder jwtDecoder,
                                                   BearerTokenResolver bearerTokenResolver) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(DOCS_ENDPOINTS).permitAll()

                        .requestMatchers(
                                "/courses",
                                "/users/all",
                                "/users/search"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                "/users/*/block",
                                "/users/*/unblock"
                        ).hasRole("ADMIN")

                        .requestMatchers(HttpMethod.GET, "/users/{id}").hasRole("ADMIN")

                        // Owner-or-admin is enforced in UserService.deleteAccount.
                        .requestMatchers(HttpMethod.DELETE, "/users/{id}").authenticated()

                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }
}