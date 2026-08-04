package org.example.petinside.cha.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    // 200 OK 성공 응답 (데이터 포함)
    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return new ApiResponse<>(status, message, data);
    }

    // 200 OK 성공 응답 (데이터 없음)
    public static <T> ApiResponse<T> success(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}