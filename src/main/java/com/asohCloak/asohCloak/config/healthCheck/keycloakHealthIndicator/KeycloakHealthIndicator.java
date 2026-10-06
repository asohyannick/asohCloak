package com.asohCloak.asohCloak.config.healthCheck.keycloakHealthIndicator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component("keycloak")
public class KeycloakHealthIndicator implements HealthIndicator {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    private final URI discoveryUri;

    public KeycloakHealthIndicator(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
        this.discoveryUri = URI.create(issuer + "/.well-known/openid-configuration");
    }

    @Override
    public Health health() {
        try {
            HttpResponse<Void> res = CLIENT.send(
                    HttpRequest.newBuilder(discoveryUri).timeout(Duration.ofSeconds(3)).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            return res.statusCode() == 200
                    ? Health.up().withDetail("issuer", discoveryUri.getHost()).build()
                    : Health.down().withDetail("status", res.statusCode()).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}