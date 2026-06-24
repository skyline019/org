package com.skyline.org.auth.dto;

import com.skyline.org.auth.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public class ResetPasswordRequest {

    @NotBlank(message = "{auth.validation.token.blank}")
    private String token;

    @NotBlank(message = "{auth.validation.password.blank}")
    @StrongPassword
    private String password;

    @NotBlank(message = "{auth.validation.confirm.blank}")
    private String confirmPassword;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
