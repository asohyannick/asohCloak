package com.asohCloak.asohCloak.service.userService;

import com.asohCloak.asohCloak.config.asyncScheduler.asyncTaskRunner.AsyncTaskRunner;
import com.asohCloak.asohCloak.config.emailTemplateMessager.EmailTemplateMessager;
import com.asohCloak.asohCloak.dto.user.*;
import com.asohCloak.asohCloak.entity.user.User;
import com.asohCloak.asohCloak.enums.UserRole;
import com.asohCloak.asohCloak.exception.badRequestException.BadRequestException;
import com.asohCloak.asohCloak.exception.keycloakAuthenticationException.KeycloakAuthenticationException;
import com.asohCloak.asohCloak.exception.notFoundRequestException.NotFoundRequestException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    private Sort resolveSort(String sortBy, String sortDirection) {
        String field = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

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
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private PagedResponseDto<UserResponseDto> toPagedResponse(Page<User> userPage) {
        List<UserResponseDto> content = userPage.getContent().stream()
                .map(this::toUserResponseDto)
                .toList();
        return new PagedResponseDto<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    public UserResponseDto register(RegisterRequestDto registerRequestDto) {
        if (userRepository.existsByEmail(registerRequestDto.email())) {
            throw new BadRequestException("Account with this email already exists.");
        }

        String keycloakUserId = keycloakAuthService.createUser(
                registerRequestDto.email(),
                registerRequestDto.firstName(),
                registerRequestDto.lastName(),
                registerRequestDto.password()
        );

        User user = userMapper.toEntity(registerRequestDto);
        String otpCode = generateOtp();

        user.setPassword(bCryptPasswordEncoder.encode(registerRequestDto.password()));
        user.setKeycloakId(keycloakUserId);
        user.setRole(UserRole.STUDENT);
        user.setAccountVerified(false);
        user.setOtpCode(otpCode);
        user.setOtpCodeVerified(false);
        user.setOtpExpiryDate(Instant.now().plus(OTP_VALIDITY_MINUTES, ChronoUnit.MINUTES));
        user.setMagicLinkExpiryDate(Instant.now());

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (RuntimeException ex) {
            keycloakAuthService.deleteUserById(keycloakUserId);
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
                savedUser.getId(), savedUser.getFirstName(), savedUser.getLastName(),
                savedUser.getEmail(), savedUser.getRole(), savedUser.isAccountVerified(),
                savedUser.isAccountLocked(), savedUser.isAccountSuspended(),
                savedUser.getCreatedAt(), savedUser.getUpdatedAt()
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
                savedUser.getCreatedAt(),
                savedUser.getUpdatedAt()
        );
    }

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
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
            tokenResponse = keycloakAuthService.login(loginRequestDto.email(), loginRequestDto.password());
        } catch (KeycloakAuthenticationException ex) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                user.setAccountLocked(true);
                user.setAccountBlocked(true);
                user.setLockedUntil(Instant.now().plus(ACCOUNT_LOCK_MINUTES, ChronoUnit.MINUTES));
            }
            userRepository.save(user);
            throw new BadRequestException("Invalid email or password.");
        }

        if (user.getFailedLoginAttempts() != 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }

        return userMapper.toLoginResponseDto(user, tokenResponse.accessToken(), tokenResponse.refreshToken());
    }

    public void logout(LogoutRequestDto logoutRequestDto) {
        keycloakAuthService.logout(logoutRequestDto.refreshToken());
    }

    public GenerateNewAccessTokenResponseDto generateNewAccessToken(GenerateNewAccessToken generateNewAccessToken) {
        KeycloakTokenResponse tokenResponse =
                keycloakAuthService.refreshAccessToken(generateNewAccessToken.refreshToken());

        String email = keycloakAuthService.getEmailFromAccessToken(tokenResponse.accessToken());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No account found for this session."));

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

        Instant now = Instant.now();
        return new GenerateNewAccessTokenResponseDto(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                now,
                now
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

        return userMapper.toLoginResponseDto(user, tokenResponse.accessToken(), tokenResponse.refreshToken());
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = "user", key = "#userId"),
            @CacheEvict(cacheNames = {"users", "userSearch"}, allEntries = true)
    })
    public void deleteOwnAccount(UUID userId, DeleteAccountRequestDto deleteAccountRequestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("No account found with this id."));

        if (user.isAccountDeleted()) {
            throw new BadRequestException("Account is already deleted.");
        }

        // Confirm the caller actually knows the password before destroying the account.
        try {
            keycloakAuthService.login(user.getEmail(), deleteAccountRequestDto.password());
        } catch (KeycloakAuthenticationException ex) {
            throw new BadRequestException("Incorrect password. Account deletion was not completed.");
        }

        // Revoke Keycloak sessions / disable the identity so old tokens stop working immediately.
        keycloakAuthService.disableUser(user.getEmail());

        user.setAccountDeleted(true);
        user.setAccountBlocked(true);
        user.setMagicLinkToken(null);
        user.setMagicLinkExpiryDate(null);
        user.setForgotPassword(null);
        user.setForgotPasswordExpiryDate(null);
        user.setOtpCode(null);
        user.setOtpExpiryDate(null);

        User savedUser = userRepository.save(user);

        asyncTaskRunner.runInBackground(
                () -> {
                    String html = EmailTemplateMessager.accountDeletedEmailAsync(
                            savedUser.getFirstName(), savedUser.getLastName(), deleteAccountRequestDto.reason());
                    return resendMailService.sendEmail(
                            savedUser.getEmail(), "Your account has been deleted - AsohClock", html);
                },
                (CreateEmailResponse response) ->
                        log.info("Account-deletion confirmation email sent to {}", savedUser.getEmail()),
                (Throwable ex) ->
                        log.error("Failed to send account-deletion confirmation email to {}: {}",
                                savedUser.getEmail(), ex.getMessage(), ex)
        );
    }

    @Cacheable(
            cacheNames = "users",
            key = "'page_' + #page + '_size_' + #size + '_sort_' + #sortBy + '_' + #sortDirection"
    )
    public PagedResponseDto<UserResponseDto> fetchUsers(int page, int size, String sortBy, String sortDirection) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, resolveSort(sortBy, sortDirection));

        Page<User> userPage = userRepository.findByAccountDeletedFalse(pageable);
        return toPagedResponse(userPage);
    }

    @Cacheable(cacheNames = "user", key = "#userId")
    public UserResponseDto fetchUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isAccountDeleted())
                .orElseThrow(() -> new NotFoundRequestException("No account found with this id."));
        return toUserResponseDto(user);
    }

    @Cacheable(
            cacheNames = "userSearch",
            key = "T(java.util.Objects).hash(#request.keyword(), #request.role(), " +
                    "#request.accountVerified(), #request.accountBlocked(), #request.accountSuspended(), " +
                    "#request.pageOrDefault(), #request.sizeOrDefault(), " +
                    "#request.sortByOrDefault(), #request.sortDirectionOrDefault())"
    )
    public PagedResponseDto<UserResponseDto> searchUsers(UserSearchRequestDto request) {
        Pageable pageable = PageRequest.of(
                request.pageOrDefault(),
                request.sizeOrDefault(),
                resolveSort(request.sortByOrDefault(), request.sortDirectionOrDefault())
        );
        Specification<User> spec = UserSpecification.build(request);
        Page<User> userPage = userRepository.findAll(spec, pageable);
        return toPagedResponse(userPage);
    }

    public UUID resolveUserIdFromEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundRequestException("No account found for the authenticated user."))
                .getId();
    }

    public LoginResponseDto loginViaGoogle(VerifyFirebaseIDTokenRequestDto verifyFirebaseIDTokenRequestDto) {
        FirebaseToken decodedToken = firebaseAuthService.verifyIdToken(verifyFirebaseIDTokenRequestDto.idToken());

        if (decodedToken.getEmail() == null || !decodedToken.isEmailVerified()) {
            throw new BadRequestException("Google account email is missing or unverified.");
        }
        String email = decodedToken.getEmail().toLowerCase();

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = provisionGoogleUser(email, decodedToken);
        } else {
            if (user.isAccountDeleted()) {
                throw new BadRequestException("Invalid email or password.");
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
            if (!user.isAccountVerified()) {
                user.setAccountVerified(true);
                user = userRepository.save(user);
            }
        }

        KeycloakTokenResponse tokenResponse = keycloakAuthService.impersonateUser(user.getEmail());
        return userMapper.toLoginResponseDto(user, tokenResponse.accessToken(), tokenResponse.refreshToken());
    }

    /**
     * First-time Google sign-in: no local account exists yet, so create one in both
     * Keycloak (source of truth) and locally, mirroring register(). A random password
     * is set in Keycloak purely to satisfy the credential requirement — Google users
     * always authenticate via token-exchange/impersonation, never the password grant.
     */
    private User provisionGoogleUser(String email, FirebaseToken decodedToken) {
        String firstName = decodedToken.getName();
        String lastName = "";
        if (firstName != null && firstName.contains(" ")) {
            int splitAt = firstName.indexOf(' ');
            lastName = firstName.substring(splitAt + 1).trim();
            firstName = firstName.substring(0, splitAt).trim();
        }

        String randomPassword = generateSecureToken();
        String keycloakUserId = keycloakAuthService.createUser(email, firstName, lastName, randomPassword);

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
            return userRepository.save(user);
        } catch (RuntimeException ex) {
            keycloakAuthService.deleteUserById(keycloakUserId);
            throw ex;
        }
    }
}