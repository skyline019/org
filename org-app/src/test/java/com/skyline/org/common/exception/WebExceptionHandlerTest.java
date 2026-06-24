package com.skyline.org.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.ui.ConcurrentModel;

import static org.assertj.core.api.Assertions.assertThat;

class WebExceptionHandlerTest {

    private final WebExceptionHandler handler = new WebExceptionHandler();

    @Test
    void rendersBusinessErrorPage() {
        Model model = new ConcurrentModel();

        String view = handler.handleBusiness(new BusinessException(ErrorCode.VALIDATION_ERROR, "bad input"), model);

        assertThat(view).isEqualTo("error/business-error");
        assertThat(model.getAttribute("errorMessage")).isEqualTo("bad input");
    }
}
