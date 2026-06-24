package com.skyline.org.auth.service;

import com.skyline.org.auth.audit.AuthAuditService;
import com.skyline.org.auth.audit.AuthEventType;
import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.common.exception.BusinessException;
import com.skyline.org.common.exception.ErrorCode;
import com.skyline.org.user.entity.User;
import com.skyline.org.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationTxService {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final AuthAuditService authAuditService;

    public RegistrationTxService(
            UserService userService,
            EmailVerificationService emailVerificationService,
            AuthAuditService authAuditService) {
        this.userService = userService;
        this.emailVerificationService = emailVerificationService;
        this.authAuditService = authAuditService;
    }

    @Transactional
    public User completeRegistration(RegisterRequest request, String passwordHash) {
        User user = userService.createUser(request.getUsername(), request.getEmail(), passwordHash);
        emailVerificationService.sendVerificationEmail(user);
        authAuditService.log(AuthEventType.REGISTER, user.getUsername(), null, user.getEmail());
        return user;
    }
}
