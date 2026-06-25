package com.skyline.org.auth.bootstrap;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.user.entity.Role;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.repository.UserRepository;
import com.skyline.org.user.service.RoleService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!prod")
@ConditionalOnProperty(prefix = "app.auth.bootstrap-admin", name = "enabled", havingValue = "true")
public class BootstrapAdminRunner implements ApplicationRunner {

    private final AuthProperties authProperties;
    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminRunner(
            AuthProperties authProperties,
            UserRepository userRepository,
            RoleService roleService,
            PasswordEncoder passwordEncoder) {
        this.authProperties = authProperties;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AuthProperties.BootstrapAdmin bootstrap = authProperties.getAuth().getBootstrapAdmin();
        if (bootstrap.getPassword() == null || bootstrap.getPassword().isBlank()) {
            throw new IllegalStateException("app.auth.bootstrap-admin.password is required when bootstrap is enabled");
        }
        if (userRepository.existsByUsername(bootstrap.getUsername())) {
            return;
        }
        Role userRole = roleService.getDefaultUserRole();
        Role adminRole = roleService.requireRole(RoleService.ADMIN_ROLE);
        User admin = new User();
        admin.setUsername(bootstrap.getUsername());
        admin.setEmail(bootstrap.getEmail());
        admin.setPasswordHash(passwordEncoder.encode(bootstrap.getPassword()));
        admin.setEnabled(true);
        admin.setEmailVerified(true);
        admin.addRole(userRole);
        admin.addRole(adminRole);
        userRepository.save(admin);
    }
}
