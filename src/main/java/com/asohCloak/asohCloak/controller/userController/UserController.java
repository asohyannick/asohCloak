package com.asohCloak.asohCloak.controller.userController;

import com.asohCloak.asohCloak.config.globalSuccessResponse.GlobalSuccessResponse;
import com.asohCloak.asohCloak.dto.user.*;
import com.asohCloak.asohCloak.enums.UserRole;
import com.asohCloak.asohCloak.service.userService.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users/*")
@RequiredArgsConstructor
@Tag(name = "Authentication and Authorization Management Endpoints",
        description = "Registration, login, and OTP verification — all identity operations are delegated to Keycloak.")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and sends a one-time verification code (OTP) to the provided email. " +
                    "The account remains unverified until the OTP is confirmed via /users/verify-otp."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully; verification OTP sent",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "An account with this email already exists, or the request failed validation")
    })
    @PostMapping("/register")
    public ResponseEntity<GlobalSuccessResponse<UserResponseDto>> register(@Valid @RequestBody RegisterRequestDto registerRequestDto) {
        UserResponseDto response = userService.register(registerRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new GlobalSuccessResponse<>(
                "Account has been created successfully. Please, check your email for the verification code sent already for activation.",
                response,
                201
        ));
    }

    @Operation(
            summary = "Verify a one-time passcode (OTP)",
            description = "Confirms the OTP sent during registration, marking the account as verified. " +
                    "The OTP is single-use and expires 10 minutes after being issued."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account successfully verified",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP code, or the account is already verified")
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<GlobalSuccessResponse<UserResponseDto>> verifyOTPCode(@Valid @RequestBody VerifyOTPCodeRequestDto verifyOTPCodeRequestDto) {
        UserResponseDto response = userService.verifyOTPCode(verifyOTPCodeRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Account has been verified successfully. Please, login now",
                response,
                201
        ));
    }

    @Operation(
            summary = "Resend the OTP verification code",
            description = "Issues a fresh 10-minute OTP code to an unverified account's email, replacing any previously issued code."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A new OTP code was generated and emailed",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "No account found with this email, or the account is already verified")
    })
    @PostMapping("/resend-otp")
    public ResponseEntity<GlobalSuccessResponse<UserResponseDto>> resendOTPVerificationCode(@Valid @RequestBody ResendOTPCodeRequestDto resendOTPCodeRequestDto) {
        UserResponseDto response = userService.resendOTPVerificationCode(resendOTPCodeRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "OTP verification code has been sent successfully to your email. Please, check your email",
                response,
                201
        ));
    }

    @Operation(
            summary = "Log in with email and password",
            description = "Authenticates against Keycloak and returns short-lived access and refresh tokens. " +
                    "Repeated failed attempts will temporarily lock the account after 5 consecutive failures."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful; tokens issued",
                    content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials, unverified email, or a blocked/suspended/locked account")
    })
    @PostMapping("/login")
    public ResponseEntity<GlobalSuccessResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto loginRequestDto) {
        LoginResponseDto response = userService.login(loginRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Login successful",
                response,
                200
        ));
    }

    @Operation(
            summary = "Generate a new access token",
            description = "Exchanges a valid, unexpired refresh token for a new access/refresh token pair. " +
                    "Requires Keycloak's \"Revoke Refresh Token\" client setting enabled so the previous " +
                    "refresh token is automatically invalidated on rotation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New tokens issued successfully",
                    content = @Content(schema = @Schema(implementation = GenerateNewAccessTokenResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Refresh token is invalid, expired, already used, " +
                    "or the associated account is blocked/suspended/deleted/locked")
    })
    @PostMapping("/refresh-token")
    public ResponseEntity<GlobalSuccessResponse<GenerateNewAccessTokenResponseDto>> generateNewAccessToken(
            @Valid @RequestBody GenerateNewAccessToken generateNewAccessToken) {
        GenerateNewAccessTokenResponseDto response = userService.generateNewAccessToken(generateNewAccessToken);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "New access token generated successfully.",
                response,
                200
        ));
    }

    @Operation(
            summary = "Log out",
            description = "Revokes the given refresh token, ending the associated session. " +
                    "Idempotent — calling this with an already-expired or already-revoked token still succeeds."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully")
    })
    @PostMapping("/logout")
    public ResponseEntity<GlobalSuccessResponse<Void>> logout(@Valid @RequestParam LogoutRequestDto logoutRequestDto) {
        userService.logout(logoutRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Logged out successfully.",
                null,
                200
        ));
    }

    @Operation(
            summary = "Request a password reset",
            description = "Sends a password-reset link to the account's email if it exists. " +
                    "Always returns the same generic message to avoid leaking whether an account exists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "If an account exists for this email, a reset link has been sent")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<GlobalSuccessResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto forgotPasswordRequestDto) {
        userService.forgetPassword(forgotPasswordRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "If an account exists for this email, a password reset link has been sent.",
                null,
                200
        ));
    }

    @Operation(
            summary = "Reset password using a reset token",
            description = "Completes a password reset using the token issued via /users/forgot-password. " +
                    "The token is single-use and expires 30 minutes after being issued."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token, or password confirmation mismatch")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<GlobalSuccessResponse<Void>> resetPassword(
            @RequestParam String token,
            @Valid @RequestBody ResetPasswordRequestDto resetPasswordRequestDto) {
        userService.resetPassword(token, resetPasswordRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Password has been reset successfully. Please log in with your new password.",
                null,
                200
        ));
    }

    @Operation(
            summary = "Block a user account (admin only)",
            description = "Administratively blocks an account, preventing login. Requires the ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account blocked successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "No account found with this id, or it is already blocked"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/block")
    public ResponseEntity<GlobalSuccessResponse<UserResponseDto>> blockAccount(
            @PathVariable UUID id,
            @Valid @RequestBody BlockAccountRequestDto blockAccountRequestDto) {
        UserResponseDto response = userService.blockAccount(id, blockAccountRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Account has been blocked successfully.",
                response,
                200
        ));
    }

    @Operation(
            summary = "Unblock a user account (admin only)",
            description = "Reverses a previous account block and clears any related login lockout. Requires the ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account unblocked successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "No account found with this id, or it is not currently blocked"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/unblock")
    public ResponseEntity<GlobalSuccessResponse<UserResponseDto>> unblockAccount(
            @PathVariable UUID id,
            @Valid @RequestBody UnblockAccountRequestDto unblockAccountRequestDto) {
        UserResponseDto response = userService.unblockAccount(id, unblockAccountRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Account has been unblocked successfully.",
                response,
                200
        ));
    }

    @Operation(
            summary = "Send a passwordless magic-link login email",
            description = "Emails a one-time sign-in link to the account if it exists and is verified. " +
                    "The link expires 15 minutes after being issued. Always returns the same generic message."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "If an eligible account exists for this email, a magic link has been sent")
    })
    @PostMapping("/send-magic-link")
    public ResponseEntity<GlobalSuccessResponse<Void>> sentMagicLinkToken(
            @Valid @RequestBody SentMagicLinkTokenRequestDto sentMagicLinkTokenRequestDto) {
        userService.sentMagicLinkToken(sentMagicLinkTokenRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "If an account exists for this email, a sign-in link has been sent.",
                null,
                200
        ));
    }

    @Operation(
            summary = "Log in via a magic-link token",
            description = "Exchanges a valid, unexpired magic-link token for access and refresh tokens. The token is single-use."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful; tokens issued",
                    content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token, or a blocked/suspended/locked account")
    })
    @PostMapping("/verify-magic-link")
    public ResponseEntity<GlobalSuccessResponse<LoginResponseDto>> loginViaMagicLinkToken(
            @Valid @RequestBody VerifyMagicLinkTokenRequestDto verifyMagicLinkTokenRequestDto) {
        LoginResponseDto response = userService.loginViaMagicLinkToken(verifyMagicLinkTokenRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Login successful",
                response,
                200
        ));
    }

    @Operation(
            summary = "Fetch all users (paginated)",
            description = "Returns a paginated, sortable list of users. Requires the ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users fetched successfully",
                    content = @Content(schema = @Schema(implementation = PagedResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<GlobalSuccessResponse<PagedResponseDto<UserResponseDto>>> fetchUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        PagedResponseDto<UserResponseDto> response = userService.fetchUsers(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(new GlobalSuccessResponse<>("Users fetched successfully.", response, 200));
    }

    @Operation(
            summary = "Fetch a single user by id",
            description = "Returns a single user's details. Requires the ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User fetched successfully",
                    content = @Content(schema = @Schema(implementation = UserResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "No account found with this id"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<GlobalSuccessResponse<UserResponseDto>> fetchUserById(@PathVariable UUID id) {
        UserResponseDto response = userService.fetchUserById(id);
        return ResponseEntity.ok(new GlobalSuccessResponse<>("User fetched successfully.", response, 200));
    }

    @Operation(
            summary = "Search users with filtering, sorting and pagination",
            description = "Filters by keyword (name/email), role, verified/blocked/suspended status; " +
                    "supports sorting and pagination. Requires the ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PagedResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "Caller does not have the ADMIN role")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public ResponseEntity<GlobalSuccessResponse<PagedResponseDto<UserResponseDto>>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean accountVerified,
            @RequestParam(required = false) Boolean accountBlocked,
            @RequestParam(required = false) Boolean accountSuspended,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        UserSearchRequestDto request = new UserSearchRequestDto(
                keyword, role, accountVerified, accountBlocked, accountSuspended,
                page, size, sortBy, sortDirection
        );
        PagedResponseDto<UserResponseDto> response = userService.searchUsers(request);
        return ResponseEntity.ok(new GlobalSuccessResponse<>("Users retrieved successfully.", response, 200));
    }

    @Operation(
            summary = "Delete my own account",
            description = "Soft-deletes the authenticated user's own account after confirming their password. " +
                    "The account and its Keycloak identity are disabled; this action cannot be undone by the user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Incorrect password, or the account is already deleted")
    })
    @DeleteMapping("/me")
    public ResponseEntity<GlobalSuccessResponse<Void>> deleteOwnAccount(
            Authentication authentication,
            @Valid @RequestBody DeleteAccountRequestDto deleteAccountRequestDto) {
        UUID userId = userService.resolveUserIdFromEmail(authentication.getName());
        userService.deleteOwnAccount(userId, deleteAccountRequestDto);
        return ResponseEntity.ok(new GlobalSuccessResponse<>(
                "Your account has been deleted successfully.",
                null,
                200
        ));
    }
}