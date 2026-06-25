package com.skyline.org.integration;

import com.skyline.org.testsupport.AbstractIntegrationTest;
import com.skyline.org.user.entity.Role;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.repository.UserRepository;
import com.skyline.org.user.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class AdminUserMvcTest extends AbstractIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired RoleService roleService;
    @Autowired PasswordEncoder passwordEncoder;

    private User targetUser;

    @BeforeEach
    void setUp() {
        Role userRole = roleService.getDefaultUserRole();
        targetUser = new User();
        targetUser.setUsername("admintarget");
        targetUser.setEmail("admintarget@example.com");
        targetUser.setPasswordHash(passwordEncoder.encode("Str0ng!Pass"));
        targetUser.setEnabled(true);
        targetUser.setEmailVerified(true);
        targetUser.addRole(userRole);
        userRepository.saveAndFlush(targetUser);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void disableUserRedirectsWithSuccessFlash() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/disable", targetUser.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("successMessage", "User disabled"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unlockUserRedirectsWithSuccessFlash() throws Exception {
        mockMvc.perform(post("/admin/users/{id}/unlock", targetUser.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("successMessage", "User unlocked"));
    }
}
