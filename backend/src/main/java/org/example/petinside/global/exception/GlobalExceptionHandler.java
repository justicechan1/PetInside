package org.example.petinside.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.petinside.global.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;


@Slf4j
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

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        return ResponseEntity
                .status(e.getStatus())
                .body(ApiResponse.<Void>of(e.getStatus(), e.getMessage(), null));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.<Void>of(HttpStatus.UNAUTHORIZED.value(), e.getMessage(), null));
    }

    @ExceptionHandler({UserNotFoundException.class, PostNotFoundException.class, CommentNotFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(RuntimeException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>of(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>of(HttpStatus.BAD_REQUEST.value(), "파일 용량은 10MB를 초과할 수 없습니다.", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<Void>of(HttpStatus.FORBIDDEN.value(), "접근 권한이 없습니다.", null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        // 위 핸들러들이 못 잡는 예외는 원인이 로그에 안 남으면 재현 없이는 진단이 불가능하므로 반드시 기록.
        log.error("처리되지 않은 서버 오류", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 오류", null));
    }
}
