package com.asohCloak.asohCloak.dto.user;
import com.asohCloak.asohCloak.validation.PasswordMatches;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload for completing a password reset")
@PasswordMatches
public record ResetPasswordRequestDto(

        @Schema(description = "New password", example = "Str0ng!Pass")
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
        )
        String newPassword,

        @Schema(description = "Confirmation of the new password — must match newPassword", example = "Str0ng!Pass")
        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) { }