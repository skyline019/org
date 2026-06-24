package com.skyline.org.auth.dto;

import com.skyline.org.auth.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "{auth.validation.username.blank}")
    @Size(min = 3, max = 50, message = "{auth.validation.username.length}")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "{auth.validation.username.format}")
    private String username;

    @NotBlank(message = "{auth.validation.email.blank}")
    @Email(message = "{auth.validation.email.format}")
    private String email;

    @NotBlank(message = "{auth.validation.password.blank}")
    @StrongPassword
    private String password;

    @NotBlank(message = "{auth.validation.confirm.blank}")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
