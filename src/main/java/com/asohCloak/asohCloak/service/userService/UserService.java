package com.asohCloak.asohCloak.service.userService;

import com.asohCloak.asohCloak.config.asyncScheduler.asyncTaskRunner.AsyncTaskRunner;
import com.asohCloak.asohCloak.config.emailTemplateMessager.EmailTemplateMessager;
import com.asohCloak.asohCloak.dto.user.*;
import com.asohCloak.asohCloak.entity.user.User;
import com.asohCloak.asohCloak.enums.UserRole;
import com.asohCloak.asohCloak.exception.badRequestException.BadRequestException;
import com.asohCloak.asohCloak.exception.keycloakAuthenticationException.KeycloakAuthenticationException;
import com.asohCloak.asohCloak.exception.notFoundRequestException.NotFoundRequestException;
import com.asohCloak.asohCloak.exception.unauthorizedRequestException.UnAuthorizedRequestException;
import com.asohCloak.asohCloak.mapper.userMappper.UserMapper;
import com.asohCloak.asohCloak.repository.userRepository.UserRepository;
import com.asohCloak.asohCloak.service.firebaseAuthService.FirebaseAuthService;
import com.asohCloak.asohCloak.service.keycloakAuthService.KeycloakAuthService;
import com.asohCloak.asohCloak.service.resendMailService.ResendMailService;
import com.asohCloak.asohCloak.utils.specification.userSpecification.UserSpecification;
import com.google.firebase.auth.FirebaseToken;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_LENGTH = 6;
    private static final long OTP_VALIDITY_MINUTES = 10;
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long ACCOUNT_LOCK_MINUTES = 15;
    private static final long FORGOT_PASSWORD_TOKEN_VALIDITY_MINUTES = 30;
    private static final long MAGIC_LINK_VALIDITY_MINUTES = 15;

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserMapper userMapper;
    private final ResendMailService resendMailService;
    private final AsyncTaskRunner asyncTaskRunner;
    private final KeycloakAuthService keycloakAuthService;
    private final FirebaseAuthService firebaseAuthService;
    private final JwtDecoder jwtDecoder;

    @Value("${app.frontend.reset-password-url}")
    private String frontendResetPasswordUrl;

    @Value("${app.frontend.login-url}")
    private String frontendLoginUrl;

    @Value("${app.frontend.magic-link-url}")
    private String frontendMagicLinkUrl;

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int code = SECURE_RANDOM.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", code);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BadRequestException("SHA-256 not available", e);
        }
    }

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "firstName", "lastName", "email", "role", "createdAt", "updatedAt"
    );

    private UserResponseDto toUserResponseDto(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.isAccountVerified(),
                user.isAccountLocked(),
                user.isAccountSuspended(),
                user.isAccountBlocked(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public UserResponseDto register(RegisterRequestDto registerRequestDto) {
        String email = registerRequestDto.email().trim().toLowerCase();
        releaseEmailIfDeleted(email);

        if (userRepository.existsByEmail(registerRequestDto.email())) {
            throw new BadRequestException("Account with this email already exists.");
        }

        String keycloakUserId = keycloakAuthService.createUser(new KeycloakCreateUserRequest(
                registerRequestDto.email(),
                registerRequestDto.firstName(),
                registerRequestDto.lastName(),
                registerRequestDto.password(),
                UserRole.STUDENT.name(),
                false
        ));

        User user = userMapper.toEntity(registerRequestDto);
        String otpCode = generateOtp();

        user.setPassword(bCryptPasswordEncoder.encode(registerRequestDto.password()));
        user.setKeycloakId(keycloakUserId);
        user.setRole(UserRole.STUDENT);
        user.setAccountVerified(false);
        user.setOtpCode(otpCode);
        user.setOtpCodeVerified(false);
        user.setAccountBlocked(false);
        user.setOtpExpiryDate(Instant.now().plus(OTP_VALIDITY_MINUTES, ChronoUnit.MINUTES));
        user.setMagicLinkExpiryDate(Instant.now());

        User savedUser;
        try {
            savedUser = userRepository.saveAndFlush(user);
        } catch (RuntimeException ex) {
            keycloakAuthService.deleteUser(keycloakUserId);
            throw ex;
        }

        asyncTaskRunner.runInBackground(
                () -> {
                    String html = EmailTemplateMessager.sendWelcomeVerificationEmailAsync(
                            savedUser.getFirstName(), savedUser.getLastName(), otpCode);
                    return resendMailService.sendEmail(
                            savedUser.getEmail(), "Verify your email - AsohClock", html);
                },
                (CreateEmailResponse response) ->
                        log.info("Verification OTP email sent to {}", savedUser.getEmail()),
                (Throwable ex) ->
                        log.error("Failed to send verification OTP email to {}: {}",
                                savedUser.getEmail(), ex.getMessage(), ex)
        );

        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.isAccountVerified(),
                savedUser.isAccountLocked(),
                savedUser.isAccountSuspended(),
                savedUser.isAccountBlocked(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()

        );
    }

    public UserResponseDto verifyOTPCode(VerifyOTPCodeRequestDto verifyOTPCodeRequestDto) {
        User user = userRepository.findByOtpCode(verifyOTPCodeRequestDto.otpCode())
                .orElseThrow(() -> new BadRequestException("Invalid OTP code."));

        if (user.isAccountVerified()) {
            throw new BadRequestException("Account is already verified.");
        }

        if (user.getOtpExpiryDate() == null || user.getOtpExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("OTP code has expired. Please request a new one.");
        }

        user.setAccountVerified(true);
        user.setOtpCodeVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiryDate(null);
        user.setAccountBlocked(false);
        String keycloakUserId = keycloakAuthService.findUserIdByEmailOrNull(user.getEmail());
        if (keycloakUserId != null) {
            keycloakAuthService.markEmailVerified(keycloakUserId);
            user.setKeycloakId(keycloakUserId);
        }

        User savedUser = userRepository.save(user);

        // Fire-and-forget confirmation email — doesn't block or affect the response.
        asyncTaskRunner.runInBackground(
                () -> {
                    String html = EmailTemplateMessager.verifyOtpCodeAsync(
                            savedUser.getFirstName(), savedUser.getLastName());
                    return resendMailService.sendEmail(
                            savedUser.getEmail(), "Email verified - AsohClock", html);
                },
                (CreateEmailResponse response) ->
                        log.info("Verification-success email sent to {}", savedUser.getEmail()),
                (Throwable ex) ->
                        log.error("Failed to send verification-success email to {}: {}",
                                savedUser.getEmail(), ex.getMessage(), ex)
        );

        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.isAccountVerified(),
                savedUser.isAccountLocked(),
                savedUser.isAccountSuspended(),
                savedUser.isAccountBlocked(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()
        );
    }

    public UserResponseDto resendOTPVerificationCode(ResendOTPCodeRequestDto resendOTPCodeRequestDto) {
        User user = userRepository.findByEmail(resendOTPCodeRequestDto.email())
                .orElseThrow(() -> new BadRequestException("No account found with this email."));

        if (user.isAccountVerified()) {
            throw new BadRequestException("Account is already verified.");
        }

        String newOtpCode = generateOtp();

        user.setOtpCode(newOtpCode);
        user.setOtpCodeVerified(false);
        user.setAccountBlocked(false);
        user.setOtpExpiryDate(Instant.now().plus(OTP_VALIDITY_MINUTES, ChronoUnit.MINUTES));

        User savedUser = userRepository.save(user);

        asyncTaskRunner.runInBackground(
                () -> {
                    String html = EmailTemplateMessager.resendOTPCodeAsync(
                            savedUser.getFirstName(), savedUser.getLastName(), newOtpCode);
                    return resendMailService.sendEmail(
                            savedUser.getEmail(), "Your new verification code - AsohClock", html);
                },
                (CreateEmailResponse response) ->
                        log.info("Resent OTP email to {}", savedUser.getEmail()),
                (Throwable ex) ->
                        log.error("Failed to resend OTP email to {}: {}",
                                savedUser.getEmail(), ex.getMessage(), ex)
        );

        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.isAccountVerified(),
                savedUser.isAccountLocked(),
                savedUser.isAccountSuspended(),
                savedUser.isAccountBlocked(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()
        );
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        String email = loginRequestDto.email().trim().toLowerCase();

        User user = userRepository.findByEmail(loginRequestDto.email())
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        if (user.isAccountDeleted()) {
            throw new BadRequestException("Invalid email or password.");
        }
        if (!user.isAccountVerified()) {
            throw new BadRequestException("Please verify your email before logging in.");
        }
        if (user.isAccountBlocked()) {
            throw new BadRequestException("Your account has been blocked. Please contact support.");
        }
        if (user.isAccountSuspended()) {
            throw new BadRequestException("Your account has been suspended.");
        }
        if (user.isAccountLocked()) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
                throw new BadRequestException("Account is temporarily locked. Please try again later.");
            }
            user.setAccountLocked(false);
            user.setFailedLoginAttempts(0);
        }

        KeycloakTokenResponse tokenResponse;
        try {
            tokenResponse = keycloakAuthService.login(email, loginRequestDto.password());
        } catch (KeycloakAuthenticationException ex) {
            if (ex.getStatus() != HttpStatus.UNAUTHORIZED) {
                throw ex;
            }
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                user.setAccountLocked(true);
                user.setAccountBlocked(true);
                user.setLockedUntil(Instant.now().plus(ACCOUNT_LOCK_MINUTES, ChronoUnit.MINUTES));
            }
            userRepository.saveAndFlush(user);
            throw new BadRequestException("Invalid email or password.");
        }

        if (user.getFailedLoginAttempts() != 0) {
            user.setFailedLoginAttempts(0);
            userRepository.saveAndFlush(user);
        }

        return LoginResponseDto.of(user, tokenResponse);
    }

    public void logout(LogoutRequestDto logoutRequestDto) {
        keycloakAuthService.logout(logoutRequestDto.refreshToken());
    }

    public GenerateNewAccessTokenResponseDto generateNewAccessToken(GenerateNewAccessToken request) {
        KeycloakTokenResponse tokens = keycloakAuthService.refreshAccessToken(request.refreshToken());

        User user;
        try {
            Jwt jwt = jwtDecoder.decode(tokens.accessToken());

            user = userRepository.findByKeycloakId(jwt.getSubject())
                    .or(() -> Optional.ofNullable(jwt.getClaimAsString("email"))
                            .map(e -> e.trim().toLowerCase())
                            .flatMap(userRepository::findByEmail))
                    .orElseThrow(() -> new BadRequestException("No account found for this session."));

            assertAccountCanHoldSession(user);
        } catch (JwtException | BadRequestException ex) {
            keycloakAuthService.logout(tokens.refreshToken());
            throw ex;
        }

        Instant now = Instant.now();
        Long refreshTtl = (tokens.refreshExpiresIn() == null || tokens.refreshExpiresIn() == 0)
                ? null : tokens.refreshExpiresIn();

        return new GenerateNewAccessTokenResponseDto(
                tokens.accessToken(),
                tokens.expiresIn(),
                tokens.expiresIn() == null ? null : now.plusSeconds(tokens.expiresIn()),
                tokens.refreshToken(),
                refreshTtl,
                refreshTtl == null ? null : now.plusSeconds(refreshTtl),
                tokens.tokenType() == null ? "Bearer" : tokens.tokenType()
        );
    }

    public void forgetPassword(ForgotPasswordRequestDto forgotPasswordRequestDto) {
        userRepository.findByEmail(forgotPasswordRequestDto.email()).ifPresent(user -> {
            if (user.isAccountDeleted() || user.isAccountBlocked()) {
                return;
            }

            String rawToken = generateSecureToken();
            user.setForgotPassword(sha256Hex(rawToken));
            user.setForgotPasswordExpiryDate(Instant.now().plus(FORGOT_PASSWORD_TOKEN_VALIDITY_MINUTES, ChronoUnit.MINUTES));

            User savedUser = userRepository.save(user);
            String resetLink = frontendResetPasswordUrl + "?token=" + rawToken;

            asyncTaskRunner.runInBackground(
                    () -> {
                        String html = EmailTemplateMessager.forgotPasswordEmailAsync(
                                savedUser.getFirstName(), savedUser.getLastName(), resetLink);
                        return resendMailService.sendEmail(
                                savedUser.getEmail(), "Reset your password - AsohClock", html);
                    },
                    (CreateEmailResponse response) ->
                            log.info("Forgot-password email sent to {}", savedUser.getEmail()),
                    (Throwable ex) ->
                            log.error("Failed to send forgot-password email to {}: {}",
                                    savedUser.getEmail(), ex.getMessage(), ex)
            );
        });
    }

    public void resetPassword(String token, ResetPasswordRequestDto resetPasswordRequestDto) {
        User user = userRepository.findByForgotPassword(sha256Hex(token))
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token."));

        if (user.getForgotPasswordExpiryDate() == null || user.getForgotPasswordExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Password reset token has expired. Please request a new one.");
        }

        keycloakAuthService.resetUserPassword(user.getEmail(), resetPasswordRequestDto.newPassword());

        user.setForgotPassword(null);
        user.setForgotPasswordExpiryDate(null);
        User savedUser = userRepository.save(user);

        asyncTaskRunner.runInBackground(
                () -> {
                    String html = EmailTemplateMessager.resetPasswordEmailAsync(
                            savedUser.getFirstName(), savedUser.getLastName(), frontendLoginUrl);
                    return resendMailService.sendEmail(
                            savedUser.getEmail(), "Password changed - AsohClock", html);
                },
                (CreateEmailResponse response) ->
                        log.info("Password-change confirmation email sent to {}", savedUser.getEmail()),
                (Throwable ex) ->
                        log.error("Failed to send password-change confirmation email to {}: {}",
                                savedUser.getEmail(), ex.getMessage(), ex)
        );
    }

    @CacheEvict(cacheNames = "user", key = "#userId")
    public UserResponseDto blockAccount(UUID userId, BlockAccountRequestDto blockAccountRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("No account found with this id."));

        if (user.isAccountBlocked()) {
            throw new BadRequestException("Account is already blocked.");
        }

        user.setAccountBlocked(true);
        User savedUser = userRepository.save(user);

        asyncTaskRunner.runInBackground(
                () -> {
                    String html = EmailTemplateMessager.blockAccountEmailAsync(
                            savedUser.getFirstName(), savedUser.getLastName(), blockAccountRequestDto.reason());
                    return resendMailService.sendEmail(
                            savedUser.getEmail(), "Account blocked - AsohClock", html);
                },
                (CreateEmailResponse response) ->
                        log.info("Account-blocked email sent to {}", savedUser.getEmail()),
                (Throwable ex) ->
                        log.error("Failed to send account-blocked email to {}: {}",
                                savedUser.getEmail(), ex.getMessage(), ex)
        );

        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.isAccountVerified(),
                savedUser.isAccountLocked(),
                savedUser.isAccountSuspended(),
                savedUser.isAccountBlocked(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()
        );
    }

    @CacheEvict(cacheNames = {"users", "userSearch"}, allEntries = true)
    public UserResponseDto unblockAccount(UUID userId, UnblockAccountRequestDto unblockAccountRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("No account found with this id."));

        if (!user.isAccountBlocked()) {
            throw new BadRequestException("Account is not currently blocked.");
        }

        user.setAccountBlocked(false);
        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setAccountBlocked(false);
        User savedUser = userRepository.save(user);

        asyncTaskRunner.runInBackground(
                () -> {
                    String html = EmailTemplateMessager.unblockAccountEmailAsync(
                            savedUser.getFirstName(), savedUser.getLastName(), frontendLoginUrl);
                    return resendMailService.sendEmail(
                            savedUser.getEmail(), "Account restored - AsohClock", html);
                },
                (CreateEmailResponse response) ->
                        log.info("Account-unblocked email sent to {}", savedUser.getEmail()),
                (Throwable ex) ->
                        log.error("Failed to send account-unblocked email to {}: {}",
                                savedUser.getEmail(), ex.getMessage(), ex)
        );

        return new UserResponseDto(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.isAccountVerified(),
                savedUser.isAccountLocked(),
                savedUser.isAccountSuspended(),
                savedUser.isAccountBlocked(),
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()
        );
    }

    public void sentMagicLinkToken(SentMagicLinkTokenRequestDto sentMagicLinkTokenRequestDto) {
        userRepository.findByEmail(sentMagicLinkTokenRequestDto.email()).ifPresent(user -> {
            if (user.isAccountDeleted() || user.isAccountBlocked() || user.isAccountSuspended()) {
                return;
            }
            if (!user.isAccountVerified()) {
                return;
            }

            String rawToken = generateSecureToken();
            user.setMagicLinkToken(sha256Hex(rawToken));
            user.setMagicLinkExpiryDate(Instant.now().plus(MAGIC_LINK_VALIDITY_MINUTES, ChronoUnit.MINUTES));

            User savedUser = userRepository.save(user);
            String magicLink = frontendMagicLinkUrl + "?token=" + rawToken;

            asyncTaskRunner.runInBackground(
                    () -> {
                        String html = EmailTemplateMessager.sentMagicLinkEmailAsync(
                                savedUser.getFirstName(), savedUser.getLastName(), magicLink);
                        return resendMailService.sendEmail(
                                savedUser.getEmail(), "Your sign-in link - AsohClock", html);
                    },
                    (CreateEmailResponse response) ->
                            log.info("Magic-link email sent to {}", savedUser.getEmail()),
                    (Throwable ex) ->
                            log.error("Failed to send magic-link email to {}: {}",
                                    savedUser.getEmail(), ex.getMessage(), ex)
            );
        });
    }

    public LoginResponseDto loginViaMagicLinkToken(VerifyMagicLinkTokenRequestDto verifyMagicLinkTokenRequestDto) {
        User user = userRepository.findByMagicLinkToken(sha256Hex(verifyMagicLinkTokenRequestDto.magicLinkToken()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired magic link."));

        if (user.getMagicLinkExpiryDate() == null || user.getMagicLinkExpiryDate().isBefore(Instant.now())) {
            throw new BadRequestException("Magic link has expired. Please request a new one.");
        }
        if (user.isAccountDeleted()) {
            throw new BadRequestException("Invalid or expired magic link.");
        }
        if (user.isAccountBlocked()) {
            throw new BadRequestException("Your account has been blocked. Please contact support.");
        }
        if (user.isAccountSuspended()) {
            throw new BadRequestException("Your account has been suspended.");
        }
        if (user.isAccountLocked() && user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new BadRequestException("Account is temporarily locked. Please try again later.");
        }

        KeycloakTokenResponse tokenResponse = keycloakAuthService.impersonateUser(user.getEmail());

        user.setMagicLinkToken(null);
        user.setMagicLinkExpiryDate(null);
        userRepository.save(user);

        return LoginResponseDto.of(user, tokenResponse);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#targetUserId"),
            @CacheEvict(cacheNames = {"users", "userSearch"}, allEntries = true)
    })
    public void deleteAccount(UUID targetUserId,
                              String callerKeycloakId,
                              String callerEmail,
                              boolean callerIsAdmin,
                              DeleteAccountRequestDto dto) {

        User caller = resolveCaller(callerKeycloakId, callerEmail);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundRequestException("No account found with this id."));

        boolean isSelf = caller.getId().equals(target.getId());
        if (!isSelf && !callerIsAdmin) {
            throw new UnAuthorizedRequestException("You are not allowed to delete this account.");
        }
        if (target.isAccountDeleted()) {
            throw new BadRequestException("Account is already deleted.");
        }

        try {
            KeycloakTokenResponse confirmation = keycloakAuthService.login(caller.getEmail(), dto.password());
            keycloakAuthService.logout(confirmation.refreshToken());
        } catch (KeycloakAuthenticationException ex) {
            throw new BadRequestException("Incorrect password. Account deletion was not completed.");
        }

        String originalEmail = target.getEmail();
        String firstName = target.getFirstName();
        String lastName = target.getLastName();

        String keycloakId = keycloakAuthService.findUserIdByEmailOrNull(originalEmail);

        tombstone(target);

        if (keycloakId != null) {
            keycloakAuthService.deleteUser(keycloakId);
        }

        log.info("Account {} deleted by {} ({}).", originalEmail, caller.getEmail(), isSelf ? "self" : "admin");

        String reason = dto.reasonOrDefault();
        asyncTaskRunner.runInBackground(
                () -> {
                    String html = EmailTemplateMessager.accountDeletedEmailAsync(firstName, lastName, reason);
                    return resendMailService.sendEmail(
                            originalEmail, "Your account has been deleted - AsohClock", html);
                },
                (CreateEmailResponse response) ->
                        log.info("Account-deletion confirmation email sent to {}", originalEmail),
                (Throwable ex) ->
                        log.error("Failed to send account-deletion confirmation email to {}: {}",
                                originalEmail, ex.getMessage(), ex)
        );
    }

    @Cacheable(cacheNames = "users", key = "#query.cacheKey()")
    public PagedResponseDto<UserResponseDto> fetchUsers(PageQuery query) {
        Page<User> userPage = userRepository.findByAccountDeletedFalse(query.toPageable());
        return PagedResponseDto.from(userPage, this::toUserResponseDto);
    }

    @Cacheable(cacheNames = "user", key = "#userId")
    public UserResponseDto fetchUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isAccountDeleted())
                .orElseThrow(() -> new NotFoundRequestException("No account found with this id."));
        return toUserResponseDto(user);
    }

    @Cacheable(cacheNames = "userSearch", key = "#request.cacheKey()")
    public PagedResponseDto<UserResponseDto> searchUsers(UserSearchRequestDto request) {
        Specification<User> spec = UserSpecification.build(request);
        Page<User> userPage = userRepository.findAll(spec, request.toPageQuery().toPageable());
        return PagedResponseDto.from(userPage, this::toUserResponseDto);
    }

    public LoginResponseDto loginViaGoogle(VerifyFirebaseIDTokenRequestDto request) {
        FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(request.idToken());

        if (decodedToken.getEmail() == null || !decodedToken.isEmailVerified()) {
            throw new BadRequestException("Google account email is missing or unverified.");
        }
        String email = decodedToken.getEmail().trim().toLowerCase();
        releaseEmailIfDeleted(email);

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = provisionGoogleUser(email, decodedToken);
        } else {
            assertAccountCanHoldSession(user);

            if (!user.isAccountVerified()) {
                user.setAccountVerified(true);
                user.setOtpCodeVerified(true);
                user.setOtpCode(null);
                user.setOtpExpiryDate(null);
            }
            String keycloakUserId = ensureKeycloakIdentity(user, decodedToken);

            keycloakAuthService.syncSeededUser(keycloakUserId, user.getRole().name());

            user = userRepository.saveAndFlush(user);
        }

        KeycloakTokenResponse tokenResponse = keycloakAuthService.impersonateUser(user.getEmail());
        return LoginResponseDto.of(user, tokenResponse);
    }

    /**
     * Resolves the user's Keycloak identity by email. Recreates it if it was removed
     * from Keycloak, and re-links the local row if the stored id is stale.
     */
    private String ensureKeycloakIdentity(User user, FirebaseToken decodedToken) {
        String keycloakUserId = keycloakAuthService.findUserIdByEmailOrNull(user.getEmail());

        if (keycloakUserId == null) {
            log.warn("Keycloak identity missing for {}; recreating it.", user.getEmail());
            keycloakUserId = keycloakAuthService.createUser(new KeycloakCreateUserRequest(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    generateSecureToken(),
                    user.getRole().name(),
                    true
            ));
        }

        if (!keycloakUserId.equals(user.getKeycloakId())) {
            log.info("Re-linking {} to Keycloak id {} (was {}).",
                    user.getEmail(), keycloakUserId, user.getKeycloakId());
            user.setKeycloakId(keycloakUserId);
        }
        return keycloakUserId;
    }


    /**
     * First-time Google sign-in: no local account exists yet, so create one in both
     * Keycloak (source of truth) and locally, mirroring register(). A random password
     * is set in Keycloak purely to satisfy the credential requirement — Google users
     * always authenticate via token-exchange/impersonation, never the password grant.
     */
    private User provisionGoogleUser(String email, FirebaseToken decodedToken) {
        String fullName = decodedToken.getName();
        String firstName;
        String lastName = "";
        if (fullName == null || fullName.isBlank()) {
            firstName = email.substring(0, email.indexOf('@'));
        } else if (fullName.trim().contains(" ")) {
            String trimmed = fullName.trim();
            int splitAt = trimmed.indexOf(' ');
            firstName = trimmed.substring(0, splitAt);
            lastName = trimmed.substring(splitAt + 1).trim();
        } else {
            firstName = fullName.trim();
        }

        String randomPassword = generateSecureToken();
        String keycloakUserId = keycloakAuthService.createUser(new KeycloakCreateUserRequest(
                email,
                firstName,
                lastName,
                randomPassword,
                UserRole.STUDENT.name(),
                true
        ));

        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPassword(bCryptPasswordEncoder.encode(randomPassword));
        user.setRole(UserRole.STUDENT);
        user.setAccountVerified(true);
        user.setOtpCodeVerified(true);
        user.setKeycloakId(keycloakUserId);
        user.setMagicLinkExpiryDate(Instant.now());

        try {
            return userRepository.saveAndFlush(user);
        } catch (RuntimeException ex) {
            keycloakAuthService.deleteUser(keycloakUserId);
            throw ex;
        }
    }

    private User resolveCaller(String keycloakId, String email) {
        Optional<User> caller = Optional.empty();
        if (keycloakId != null && !keycloakId.isBlank()) {
            caller = userRepository.findByKeycloakId(keycloakId);
        }
        if (caller.isEmpty() && email != null && !email.isBlank()) {
            caller = userRepository.findByEmail(email.trim().toLowerCase());
        }
        User user = caller.orElseThrow(() ->
                new NotFoundRequestException("No account found for the authenticated user."));
        if (user.isAccountDeleted()) {
            throw new UnAuthorizedRequestException("This session belongs to a deleted account.");
        }
        return user;
    }

    private void assertAccountCanHoldSession(User user) {
        if (user.isAccountDeleted()) {
            throw new BadRequestException("Invalid session. Please log in again.");
        }
        if (user.isAccountBlocked()) {
            throw new BadRequestException("Your account has been blocked. Please contact support.");
        }
        if (user.isAccountSuspended()) {
            throw new BadRequestException("Your account has been suspended.");
        }
        if (user.isAccountLocked() && user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new BadRequestException("Account is temporarily locked. Please try again later.");
        }
    }

    /** Anonymises a deleted account: keeps the row for audit but releases its email. */
    private void tombstone(User user) {
        if (user.getDeletedEmail() == null) {
            user.setDeletedEmail(user.getEmail());
        }
        if (user.getDeletedAt() == null) {
            user.setDeletedAt(Instant.now());
        }
        user.setEmail("deleted-" + user.getId() + "@deleted.invalid");
        user.setKeycloakId(null);
        user.setAccountDeleted(true);
        user.setAccountBlocked(true);
        user.setPassword("!deleted");
        user.setMagicLinkToken(null);
        user.setMagicLinkExpiryDate(null);
        user.setForgotPassword(null);
        user.setForgotPasswordExpiryDate(null);
        user.setOtpCode(null);
        user.setOtpExpiryDate(null);
        userRepository.saveAndFlush(user);
    }

    private void releaseEmailIfDeleted(String email) {
        userRepository.findByEmail(email)
                .filter(User::isAccountDeleted)
                .ifPresent(old -> {
                    log.info("Releasing email {} from deleted account {}.", email, old.getId());
                    tombstone(old);
                });
    }
}