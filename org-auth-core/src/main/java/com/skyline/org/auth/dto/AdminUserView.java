package com.skyline.org.auth.dto;

import java.util.List;

public record AdminUserView(
        Long id,
        String username,
        String email,
        boolean enabled,
        boolean emailVerified,
        boolean locked,
        List<String> roles
) {
}
