package org.example.petinside.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.auth.dto.LoginRequest;
import org.example.petinside.domain.auth.dto.LoginResponse;
import org.example.petinside.domain.auth.dto.SignupRequest;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.DuplicateFieldException;
import org.example.petinside.global.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "아이디 또는 비밀번호가 일치하지 않습니다.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateFieldException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new DuplicateFieldException("이미 사용 중인 닉네임입니다.");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.createLocalUser(request.username(), encodedPassword, request.nickname());

        userRepository.save(user);
    }

    // 로그인 service
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 사용자 유무 확인
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        // 비밀번호 확인
        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        return new LoginResponse(user.getId(), user.getUsername(), user.getNickname());
    }
}
