package com.skyline.org.auth.service;

import com.skyline.org.auth.dto.RegisterRequest;
import com.skyline.org.common.exception.BusinessException;
import com.skyline.org.common.exception.ErrorCode;
import com.skyline.org.common.i18n.Messages;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

    private final RegistrationTxService registrationTxService;
    private final PasswordEncoder passwordEncoder;
    private final Messages messages;

    public RegistrationService(
            RegistrationTxService registrationTxService,
            PasswordEncoder passwordEncoder,
            Messages messages) {
        this.registrationTxService = registrationTxService;
        this.passwordEncoder = passwordEncoder;
        this.messages = messages;
    }

    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, messages.get("auth.register.mismatch"));
        }
        String passwordHash = passwordEncoder.encode(request.getPassword());
        registrationTxService.completeRegistration(request, passwordHash);
    }
}
