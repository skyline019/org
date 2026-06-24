package com.skyline.org.common.response;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, String message, String code, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> fail(String message) {
        return fail(null, message);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, message, code, Instant.now());
    }
}
