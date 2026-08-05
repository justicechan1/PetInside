package org.example.petinside.global.exception;

import org.example.petinside.global.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>of(HttpStatus.BAD_REQUEST.value(), message, null));
    }

    @ExceptionHandler(DuplicateFieldException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateField(DuplicateFieldException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>of(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
    }

    // 동시 요청으로 중복 체크(existsBy...)를 둘 다 통과한 뒤,
    // DB의 UNIQUE 제약(username, nickname)에서 걸리는 경우를 대비한 안전장치입니다.
    // 이 예외를 안 잡으면 500으로 나가는데, 실제로는 "중복 가입" 상황이므로 400으로 맞춰줍니다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>of(
                        HttpStatus.BAD_REQUEST.value(),
                        "이미 사용 중인 아이디 또는 닉네임입니다.",
                        null
                ));
    }
}
