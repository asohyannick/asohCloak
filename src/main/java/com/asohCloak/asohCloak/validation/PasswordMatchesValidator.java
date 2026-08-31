package com.asohCloak.asohCloak.validation;
import com.asohCloak.asohCloak.dto.user.ResetPasswordRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, ResetPasswordRequestDto> {

    @Override
    public boolean isValid(ResetPasswordRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null || dto.newPassword() == null || dto.confirmPassword() == null) {
            return true;
        }
        boolean matches = dto.newPassword().equals(dto.confirmPassword());
        if (!matches) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Passwords do not match")
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }
        return matches;
    }
}