package com.skyline.org.user.service;

import com.skyline.org.common.exception.BusinessException;
import com.skyline.org.common.exception.ErrorCode;
import com.skyline.org.common.i18n.Messages;
import com.skyline.org.user.entity.Role;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final Messages messages;

    public UserService(UserRepository userRepository, RoleService roleService, Messages messages) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsernameForLogin(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public User findByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, messages.get("auth.error.user-not-found")));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public User createUser(String username, String email, String passwordHash) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_TAKEN, messages.get("auth.error.username-taken"));
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_TAKEN, messages.get("auth.error.email-taken"));
        }

        Role userRole = roleService.getDefaultUserRole();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setEnabled(false);
        user.setEmailVerified(false);
        user.addRole(userRole);
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            if (userRepository.existsByUsername(username)) {
                throw new BusinessException(ErrorCode.USERNAME_TAKEN, messages.get("auth.error.username-taken"));
            }
            throw new BusinessException(ErrorCode.EMAIL_TAKEN, messages.get("auth.error.email-taken"));
        }
    }

    @Transactional
    public void verifyEmail(User user) {
        user.setEmailVerified(true);
        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void updatePassword(User user, String passwordHash) {
        user.setPasswordHash(passwordHash);
        userRepository.save(user);
    }

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }
}
