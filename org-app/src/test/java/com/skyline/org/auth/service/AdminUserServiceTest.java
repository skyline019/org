package com.skyline.org.auth.service;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.common.exception.BusinessException;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.user.entity.Role;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.repository.UserRepository;
import com.skyline.org.user.service.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleService roleService;
    @Mock AccountLockService accountLockService;
    @Mock AuthAuditService authAuditService;
    @Mock Messages messages;

    @InjectMocks AdminUserService adminUserService;

    @Test
    void listsUsersWithRoles() {
        User user = sampleUser();
        when(userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<com.skyline.org.auth.dto.AdminUserView> page = adminUserService.listUsers(PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).username()).isEqualTo("alice");
        assertThat(page.getContent().get(0).roles()).contains("ROLE_USER");
    }

    @Test
    void unlocksUser() {
        User user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminUserService.unlockUser(1L, "admin");

        verify(accountLockService).resetLockState(user);
        verify(authAuditService).log(
                com.skyline.org.auth.audit.AuthEventType.ADMIN_USER_UPDATED,
                "admin", null, "unlock,target=alice");
    }

    @Test
    void disablesUser() {
        User user = sampleUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminUserService.setEnabled(1L, false, "admin");

        verify(userRepository).save(user);
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void revokesAdminRole() {
        User user = sampleUser();
        Role adminRole = new Role();
        adminRole.setName(RoleService.ADMIN_ROLE);
        user.addRole(adminRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countByRoleName(RoleService.ADMIN_ROLE)).thenReturn(2L);

        adminUserService.revokeAdminRole(1L, "admin");

        assertThat(user.getRoles()).noneMatch(role -> RoleService.ADMIN_ROLE.equals(role.getName()));
    }

    @Test
    void grantsAdminRole() {
        User user = sampleUser();
        Role adminRole = new Role();
        adminRole.setName(RoleService.ADMIN_ROLE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleService.requireRole(RoleService.ADMIN_ROLE)).thenReturn(adminRole);

        adminUserService.grantAdminRole(1L, "admin");

        assertThat(user.getRoles()).contains(adminRole);
        verify(userRepository).save(user);
    }

    @Test
    void rejectsDisablingLastEnabledAdmin() {
        User user = adminUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countEnabledByRoleName(RoleService.ADMIN_ROLE)).thenReturn(1L);
        when(messages.get("auth.error.last-admin")).thenReturn("last admin");

        assertThatThrownBy(() -> adminUserService.setEnabled(1L, false, "admin"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsRevokingLastAdminRole() {
        User user = adminUser();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.countByRoleName(RoleService.ADMIN_ROLE)).thenReturn(1L);
        when(messages.get("auth.error.last-admin")).thenReturn("last admin");

        assertThatThrownBy(() -> adminUserService.revokeAdminRole(1L, "admin"))
                .isInstanceOf(BusinessException.class);
    }

    private static User adminUser() {
        User user = sampleUser();
        Role adminRole = new Role();
        adminRole.setName(RoleService.ADMIN_ROLE);
        user.addRole(adminRole);
        return user;
    }

    private static User sampleUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setEnabled(true);
        user.setEmailVerified(true);
        Role role = new Role();
        role.setName("ROLE_USER");
        user.setRoles(new java.util.HashSet<>(Set.of(role)));
        return user;
    }
}
