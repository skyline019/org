package com.skyline.org.user.service;

import com.skyline.org.user.entity.Role;
import com.skyline.org.user.repository.RoleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    public static final String DEFAULT_USER_ROLE = "ROLE_USER";

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable("roles")
    public Role getDefaultUserRole() {
        return roleRepository.findByName(DEFAULT_USER_ROLE)
                .orElseThrow(() -> new IllegalStateException(DEFAULT_USER_ROLE + " not found"));
    }
}
