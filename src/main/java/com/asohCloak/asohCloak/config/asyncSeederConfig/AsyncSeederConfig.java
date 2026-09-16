package com.asohCloak.asohCloak.config.asyncSeederConfig;

import com.asohCloak.asohCloak.config.asyncSeederConfig.userSeedCredential.UserSeedCredential;
import com.asohCloak.asohCloak.entity.user.User;
import com.asohCloak.asohCloak.enums.UserRole;
import com.asohCloak.asohCloak.repository.userRepository.UserRepository;
import com.asohCloak.asohCloak.service.keycloakAuthService.KeycloakAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class AsyncSeederConfig implements ApplicationRunner {

    private final Map<String, UserSeedCredential> userSeedCredentials;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakAuthService keycloakAuthService;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        List<String> roleNames = Arrays.stream(UserRole.values())
                .map(Enum::name)
                .toList();

        log.info("Ensuring {} realm role(s) exist in Keycloak.", roleNames.size());
        keycloakAuthService.ensureRealmRolesExist(roleNames);
        log.info("Realm role provisioning complete.");

        log.info("User seeding started: {} role(s) configured.", userSeedCredentials.size());

        int created = 0;
        int reconciled = 0;
        int skippedInvalid = 0;
        int failed = 0;

        for (Map.Entry<String, UserSeedCredential> entry : userSeedCredentials.entrySet()) {
            String roleKey = entry.getKey();
            UserSeedCredential credential = entry.getValue();

            if (credential == null || isBlank(credential.getEmail()) || isBlank(credential.getPassword())) {
                log.warn("Skipping seed for '{}': email or password is missing or unresolved.", roleKey);
                skippedInvalid++;
                continue;
            }

            UserRole role = resolveRole(roleKey);
            if (role == null) {
                log.warn("Skipping seed for '{}': no matching UserRole enum constant found.", roleKey);
                skippedInvalid++;
                continue;
            }

            String email = credential.getEmail().trim().toLowerCase();

            try {
                boolean wasCreated = seedOrReconcile(roleKey, role, email, credential.getPassword());
                if (wasCreated) {
                    created++;
                } else {
                    reconciled++;
                }
            } catch (RuntimeException ex) {
                log.error("Failed to seed/reconcile {} ({}): {}", email, role, ex.getMessage(), ex);
                failed++;
            }
        }

        log.info(
                "User seeding complete. created={}, reconciled={}, skippedInvalid={}, failed={}",
                created, reconciled, skippedInvalid, failed
        );
    }

    private boolean seedOrReconcile(String roleKey, UserRole role, String email, String rawPassword) {
        boolean created = false;

        String keycloakUserId = keycloakAuthService.findUserIdByEmailOrNull(email);
        if (keycloakUserId == null) {
            keycloakUserId = keycloakAuthService.createUser(
                    email,
                    deriveFirstName(roleKey),
                    deriveLastName(roleKey),
                    rawPassword,
                    role.name()
            );
            created = true;
            log.info("Created Keycloak user {} with role {}.", email, role);
        }

        keycloakAuthService.syncSeededUser(keycloakUserId, role.name());

        User existingLocal = userRepository.findByEmail(email).orElse(null);
        if (existingLocal != null && existingLocal.isAccountDeleted()) {
            log.info("Skipping reconcile for deleted account {}.", email);
            return false;
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = buildUser(roleKey, role, email, rawPassword);
            user.setKeycloakId(keycloakUserId);
            userRepository.save(user);
            log.info("Created local record for {} with role {}.", email, role);
        } else if (!keycloakUserId.equals(user.getKeycloakId()) || user.getRole() != role) {
            user.setKeycloakId(keycloakUserId);
            user.setRole(role);
            userRepository.save(user);
            log.info("Re-linked local record for {} to Keycloak id {} with role {}.", email, keycloakUserId, role);
        }

        return created;
    }

    private UserRole resolveRole(String roleKey) {
        try {
            String enumName = roleKey.trim().toUpperCase().replace("-", "_");
            return UserRole.valueOf(enumName);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private User buildUser(String roleKey, UserRole role, String email, String rawPassword) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setFirstName(deriveFirstName(roleKey));
        user.setLastName(deriveLastName(roleKey));

        user.setAccountVerified(true);
        user.setAccountBlocked(false);
        user.setAccountDeleted(false);
        user.setAccountSuspended(false);
        user.setAccountLocked(false);
        user.setOtpCodeVerified(true);

        user.setMagicLinkExpiryDate(Instant.now());

        return user;
    }

    private String deriveFirstName(String roleKey) {
        String[] parts = roleKey.split("-");
        return capitalize(parts[0]);
    }

    private String deriveLastName(String roleKey) {
        String[] parts = roleKey.split("-");
        if (parts.length <= 1) {
            return capitalize(parts[0]);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) {
                sb.append(" ");
            }
            sb.append(capitalize(parts[i]));
        }
        return sb.toString();
    }

    private String capitalize(String word) {
        if (word == null || word.isBlank()) {
            return word;
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank() || value.startsWith("${");
    }
}