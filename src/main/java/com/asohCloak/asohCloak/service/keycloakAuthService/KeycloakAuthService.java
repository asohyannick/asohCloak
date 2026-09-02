package com.asohCloak.asohCloak.service.keycloakAuthService;

import com.asohCloak.asohCloak.config.securityConfig.keycloakProperties.KeycloakProperties;
import com.asohCloak.asohCloak.dto.user.KeycloakTokenResponse;
import com.asohCloak.asohCloak.exception.badRequestException.BadRequestException;
import com.asohCloak.asohCloak.exception.keycloakAuthenticationException.KeycloakAuthenticationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KeycloakAuthService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthService.class);

    private final RestClient keycloakRestClient;
    private final KeycloakProperties keycloakProperties;

    /**
     * Creates the user's identity in Keycloak via the Admin REST API. This is the
     * canonical account creation step — the local User row is a read-optimized
     * mirror, not the source of truth.
     */
    public String createUser(String email, String firstName, String lastName, String password) {
        String adminToken = getAdminAccessToken();

        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        );

        Map<String, Object> userPayload = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", false,
                "credentials", List.of(credential)
        );

        String createUri = "/admin/realms/" + keycloakProperties.getRealm() + "/users";

        try {
            ResponseEntity<Void> response = keycloakRestClient.post()
                    .uri(createUri)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userPayload)
                    .retrieve()
                    .toBodilessEntity();

            URI location = response.getHeaders().getLocation();
            if (location == null) {
                throw new KeycloakAuthenticationException("Keycloak did not return a location for the created user.");
            }
            String path = location.getPath();
            return path.substring(path.lastIndexOf('/') + 1);

        } catch (HttpClientErrorException.Conflict e) {
            log.warn("Keycloak user creation conflict for {}: {}", email, e.getMessage());
            throw new BadRequestException("Account with this email already exists.");
        } catch (RestClientException e) {
            log.error("Keycloak user creation failed for {}: {}", email, e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to create account at this time.", e);
        }
    }

    /**
     * Best-effort compensating action: deletes a just-created Keycloak user if the
     * local persistence step afterward fails, to avoid an orphaned identity with
     * no matching local record. Failures here are logged, not thrown — we don't
     * want a cleanup failure to mask the original error.
     */
    public void deleteUserById(String keycloakUserId) {
        String adminToken;
        try {
            adminToken = getAdminAccessToken();
        } catch (RuntimeException e) {
            log.error("Could not obtain admin token to roll back Keycloak user {}: {}", keycloakUserId, e.getMessage(), e);
            return;
        }

        String userUri = "/admin/realms/" + keycloakProperties.getRealm() + "/users/" + keycloakUserId;
        try {
            keycloakRestClient.delete()
                    .uri(userUri)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Rolled back orphaned Keycloak user {} after registration failure.", keycloakUserId);
        } catch (RestClientException e) {
            log.error("Failed to roll back Keycloak user {} after registration failure: {}", keycloakUserId, e.getMessage(), e);
        }
    }

    /**
     * Resource Owner Password Credentials (direct access grant) login.
     * Requires "Direct Access Grants Enabled" on the Keycloak client.
     */
    public KeycloakTokenResponse login(String email, String password) {
        String tokenUri = "/realms/" + keycloakProperties.getRealm() + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("username", email);
        form.add("password", password);


        try {
            return keycloakRestClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
            log.warn("Keycloak login rejected for {}: {}", email, e.getMessage());
            throw new KeycloakAuthenticationException("Invalid email or password.", e);
        } catch (RestClientException e) {
            log.error("Keycloak token endpoint call failed for {}: {}", email, e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to reach authentication server.", e);
        }
    }

    /**
     * Sets a new permanent password for the given user via the Keycloak Admin REST API.
     */
    public void resetUserPassword(String email, String newPassword) {
        String adminToken = getAdminAccessToken();
        String userId = findUserIdByEmail(email, adminToken);

        Map<String, Object> credentialPayload = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", false
        );

        String resetUri = "/admin/realms/" + keycloakProperties.getRealm() + "/users/" + userId + "/reset-password";

        try {
            keycloakRestClient.put()
                    .uri(resetUri)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(credentialPayload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Keycloak password reset failed for {}: {}", email, e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to reset password at this time.", e);
        }
    }

    public KeycloakTokenResponse impersonateUser(String email) {
        String adminToken = getAdminAccessToken();
        String userId = findUserIdByEmail(email, adminToken);

        String tokenUri = "/realms/" + keycloakProperties.getRealm() + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("subject_token", adminToken);
        form.add("requested_subject", userId);

        try {
            KeycloakTokenResponse response = keycloakRestClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new KeycloakAuthenticationException("Unable to complete magic-link login.");
            }
            return response;
        } catch (HttpClientErrorException e) {
            log.error("Keycloak token exchange failed for {}: {}", email, e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to complete magic-link login.", e);
        } catch (RestClientException e) {
            log.error("Keycloak token endpoint call failed during token exchange for {}: {}", email, e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to reach authentication server.", e);
        }
    }

    /**
     * Disables the Keycloak user account (soft-disable, not a hard delete) and
     * revokes all active sessions so previously issued tokens stop working immediately.
     */
    public void disableUser(String email) {
        String adminToken = getAdminAccessToken();
        String userId = findUserIdByEmail(email, adminToken);

        Map<String, Object> disablePayload = Map.of("enabled", false);
        String userUri = "/admin/realms/" + keycloakProperties.getRealm() + "/users/" + userId;

        try {
            keycloakRestClient.put()
                    .uri(userUri)
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(disablePayload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Keycloak account disable failed for {}: {}", email, e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to disable account at this time.", e);
        }

        revokeUserSessions(userId, adminToken, email);
    }

    /**
     * Revokes the given refresh token, ending the session it belongs to.
     * Uses the standard OIDC RP-initiated logout endpoint — no admin token needed,
     * since a valid refresh token is itself sufficient proof of ownership.
     */
    public void logout(String refreshToken) {
        String logoutUri = "/realms/" + keycloakProperties.getRealm() + "/protocol/openid-connect/logout";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("refresh_token", refreshToken);

        try {
            keycloakRestClient.post()
                    .uri(logoutUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.BadRequest e) {
            log.warn("Keycloak logout called with an already-invalid refresh token: {}", e.getMessage());
        } catch (RestClientException e) {
            log.error("Keycloak logout call failed: {}", e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to log out at this time.", e);
        }
    }

    /**
     * Exchanges a valid, unexpired refresh token for a new access/refresh token pair.
     * Relies on Keycloak's own refresh-token grant validation for expiry/revocation checks.
     * Old-token invalidation on rotation is enforced by the "Revoke Refresh Token" setting
     * on the Keycloak client, not by this method.
     */
    public KeycloakTokenResponse refreshAccessToken(String refreshToken) {
        String tokenUri = "/realms/" + keycloakProperties.getRealm() + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());
        form.add("refresh_token", refreshToken);

        try {
            KeycloakTokenResponse response = keycloakRestClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (response == null || response.accessToken() == null || response.refreshToken() == null) {
                throw new KeycloakAuthenticationException("Unable to generate a new access token.");
            }
            return response;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.BadRequest e) {
            log.warn("Keycloak refresh-token grant rejected: {}", e.getMessage());
            throw new KeycloakAuthenticationException("Refresh token is invalid, expired, or has already been used. Please log in again.", e);
        } catch (RestClientException e) {
            log.error("Keycloak token endpoint call failed during refresh: {}", e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to reach authentication server.", e);
        }
    }

    /**
     * Resolves the account email tied to a freshly issued access token via Keycloak's
     * userinfo endpoint, so the caller can re-check current account status (blocked/
     * suspended/deleted) before honoring the refresh.
     */
    public String getEmailFromAccessToken(String accessToken) {
        String userInfoUri = "/realms/" + keycloakProperties.getRealm() + "/protocol/openid-connect/userinfo";

        try {
            Map<String, Object> userInfo = keycloakRestClient.get()
                    .uri(userInfoUri)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (userInfo == null || userInfo.get("email") == null) {
                throw new KeycloakAuthenticationException("Unable to resolve account for this token.");
            }
            return String.valueOf(userInfo.get("email"));
        } catch (RestClientException e) {
            log.error("Keycloak userinfo call failed: {}", e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to reach authentication server.", e);
        }
    }

    private void revokeUserSessions(String userId, String adminToken, String email) {
        String logoutUri = "/admin/realms/" + keycloakProperties.getRealm() + "/users/" + userId + "/logout";

        try {
            keycloakRestClient.post()
                    .uri(logoutUri)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Keycloak session revocation failed for {}: {}", email, e.getMessage(), e);
        }
    }

    private String getAdminAccessToken() {
        String tokenUri = "/realms/" + keycloakProperties.getRealm() + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloakProperties.getClientId());
        form.add("client_secret", keycloakProperties.getClientSecret());

        try {
            KeycloakTokenResponse response = keycloakRestClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new KeycloakAuthenticationException("Unable to obtain admin access token.");
            }
            return response.accessToken();
        } catch (RestClientException e) {
            log.error("Failed to obtain Keycloak admin access token: {}", e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to reach authentication server.", e);
        }
    }

    private String findUserIdByEmail(String email, String adminToken) {
        String searchUri = "/admin/realms/" + keycloakProperties.getRealm()
                + "/users?email=" + email + "&exact=true";

        try {
            List<Map<String, Object>> users = keycloakRestClient.get()
                    .uri(searchUri)
                    .header("Authorization", "Bearer " + adminToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (users == null || users.isEmpty()) {
                throw new KeycloakAuthenticationException("No matching Keycloak account found for this email.");
            }
            return String.valueOf(users.get(0).get("id"));
        } catch (RestClientException e) {
            log.error("Keycloak user lookup failed for {}: {}", email, e.getMessage(), e);
            throw new KeycloakAuthenticationException("Unable to reach authentication server.", e);
        }
    }
}