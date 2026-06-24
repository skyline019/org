package com.skyline.org.common.exception;

import com.skyline.org.common.i18n.Messages;
import com.skyline.org.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        StaticMessageSource source = new StaticMessageSource();
        source.addMessage("auth.error.internal", Locale.getDefault(), "internal error");
        handler = new GlobalExceptionHandler(new Messages(source));
    }

    @Test
    void handlesBusinessException() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(
                new BusinessException(ErrorCode.USERNAME_TAKEN, "taken"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.USERNAME_TAKEN.name());
        assertThat(response.getBody().message()).isEqualTo("taken");
    }

    @Test
    void handlesValidationException() throws NoSuchMethodException {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "username", "required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.VALIDATION_ERROR.name());
        assertThat(response.getBody().message()).contains("required");
    }

    @Test
    void handlesMissingStaticResource() {
        ResponseEntity<Void> response = handler.handleNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/missing.js", "static"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handlesUnexpectedException() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.INTERNAL_ERROR.name());
        assertThat(response.getBody().message()).isEqualTo("internal error");
    }
}
