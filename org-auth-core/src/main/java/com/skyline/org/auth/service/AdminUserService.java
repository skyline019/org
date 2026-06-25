package com.skyline.org.auth.service;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.dto.AdminUserView;
import com.skyline.org.auth.lock.AccountLockService;
import com.skyline.org.common.exception.BusinessException;
import com.skyline.org.common.exception.ErrorCode;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.user.entity.Role;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.repository.UserRepository;
import com.skyline.org.user.service.RoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final AccountLockService accountLockService;
    private final AuthAuditService authAuditService;
    private final Messages messages;

    public AdminUserService(
            UserRepository userRepository,
            RoleService roleService,
            AccountLockService accountLockService,
            AuthAuditService authAuditService,
            Messages messages) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.accountLockService = accountLockService;
        this.authAuditService = authAuditService;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserView> listUsers(Pageable pageable) {
        return userRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toView);
    }

    @Transactional
    public void setEnabled(Long userId, boolean enabled, String actor) {
        User user = requireUser(userId);
        if (!enabled) {
            ensureNotLastEnabledAdmin(user);
        }
        user.setEnabled(enabled);
        userRepository.save(user);
        authAuditService.log(AuthEventType.ADMIN_USER_UPDATED, actor, null,
                "enabled=" + enabled + ",target=" + user.getUsername());
    }

    @Transactional
    public void unlockUser(Long userId, String actor) {
        User user = requireUser(userId);
        accountLockService.resetLockState(user);
        authAuditService.log(AuthEventType.ADMIN_USER_UPDATED, actor, null,
                "unlock,target=" + user.getUsername());
    }

    @Transactional
    public void grantAdminRole(Long userId, String actor) {
        User user = requireUser(userId);
        Role adminRole = roleService.requireRole(RoleService.ADMIN_ROLE);
        user.addRole(adminRole);
        userRepository.save(user);
        authAuditService.log(AuthEventType.ADMIN_USER_UPDATED, actor, null,
                "grantAdmin,target=" + user.getUsername());
    }

    @Transactional
    public void revokeAdminRole(Long userId, String actor) {
        User user = requireUser(userId);
        if (hasAdminRole(user)) {
            ensureNotLastAdmin(user);
        }
        user.getRoles().removeIf(role -> RoleService.ADMIN_ROLE.equals(role.getName()));
        userRepository.save(user);
        authAuditService.log(AuthEventType.ADMIN_USER_UPDATED, actor, null,
                "revokeAdmin,target=" + user.getUsername());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, messages.get("auth.error.user-not-found")));
    }

    private void ensureNotLastAdmin(User user) {
        if (hasAdminRole(user) && userRepository.countByRoleName(RoleService.ADMIN_ROLE) <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN, messages.get("auth.error.last-admin"));
        }
    }

    private void ensureNotLastEnabledAdmin(User user) {
        if (hasAdminRole(user) && user.isEnabled()
                && userRepository.countEnabledByRoleName(RoleService.ADMIN_ROLE) <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN, messages.get("auth.error.last-admin"));
        }
    }

    private static boolean hasAdminRole(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> RoleService.ADMIN_ROLE.equals(role.getName()));
    }

    private AdminUserView toView(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new AdminUserView(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.isLocked(),
                roles);
    }
}
