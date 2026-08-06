package org.example.petinside.global.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("존재하지 않은 회원입니다. id=" + userId);
    }
}
