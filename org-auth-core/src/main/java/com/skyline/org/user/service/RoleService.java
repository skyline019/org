package com.skyline.org.user.service;

import com.skyline.org.user.entity.Role;
import com.skyline.org.user.repository.RoleRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    public static final String DEFAULT_USER_ROLE = "ROLE_USER";
    public static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable("roles")
    public Role getDefaultUserRole() {
        return requireRole(DEFAULT_USER_ROLE);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#name")
    public Role requireRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalStateException(name + " not found"));
    }
}
