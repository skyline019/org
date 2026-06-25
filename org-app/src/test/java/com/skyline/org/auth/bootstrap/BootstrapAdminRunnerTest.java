package com.skyline.org.auth.bootstrap;

import com.skyline.org.auth.config.AuthProperties;
import com.skyline.org.user.repository.UserRepository;
import com.skyline.org.user.service.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminRunnerTest {

    @Mock UserRepository userRepository;
    @Mock RoleService roleService;
    @Mock PasswordEncoder passwordEncoder;

    @Test
    void skipsWhenAdminAlreadyExists() throws Exception {
        AuthProperties properties = new AuthProperties();
        properties.getAuth().getBootstrapAdmin().setEnabled(true);
        properties.getAuth().getBootstrapAdmin().setUsername("admin");
        properties.getAuth().getBootstrapAdmin().setPassword("Secret123!");
        BootstrapAdminRunner bootstrapRunner = new BootstrapAdminRunner(
                properties, userRepository, roleService, passwordEncoder);
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        bootstrapRunner.run(null);

        verify(userRepository, never()).save(any());
    }
}
