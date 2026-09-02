package com.asohCloak.asohCloak.config.asyncSeederConfig;

import com.asohCloak.asohCloak.config.asyncSeederConfig.userSeedCredential.UserSeedCredential;
import com.asohCloak.asohCloak.entity.user.User;
import com.asohCloak.asohCloak.enums.UserRole;
import com.asohCloak.asohCloak.repository.userRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Seeds one default {@link User} per role declared under `users.*` in
 * application.yaml, hashing each raw password before persisting.
 *
 * Runs once via {@link ApplicationRunner}, right after the context is fully
 * initialized. It is idempotent by email: on every subsequent application
 * restart, roles whose email already exists in the database are skipped, so
 * duplicate accounts are never created and passwords are never re-hashed
 * or overwritten on restart.
 *
 * Role resolution: each YAML key (e.g. "quality-assurance-manager") is
 * converted to SCREAMING_SNAKE_CASE ("QUALITY_ASSURANCE_MANAGER") and
 * resolved against {@link UserRole}. This works because every key in the
 * `users` block was named to match its UserRole constant exactly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class AsyncSeederConfig implements ApplicationRunner {

    private final Map<String, UserSeedCredential> userSeedCredentials;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        log.info("User seeding started: {} role(s) configured.", userSeedCredentials.size());

        int seeded = 0;
        int alreadyExisted = 0;
        int skippedInvalid = 0;

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

            if (userRepository.existsByEmail(email)) {
                alreadyExisted++;
                continue;
            }

            User user = buildUser(roleKey, role, email, credential.getPassword());
            userRepository.save(user);
            seeded++;
            log.info("Seeded default user [{}] with role {}.", email, role);
        }

        log.info(
                "User seeding complete. seeded={}, alreadyExisted={}, skippedInvalid={}",
                seeded, alreadyExisted, skippedInvalid
        );
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