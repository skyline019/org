package com.skyline.org.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }
        PasswordStrengthResult result = PasswordStrengthChecker.check(password);
        if (!result.valid()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{" + result.message() + "}")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
