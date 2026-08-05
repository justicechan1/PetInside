package org.example.petinside.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.auth.dto.SignupRequest;
import org.example.petinside.domain.user.User;
import org.example.petinside.domain.user.UserRepository;
import org.example.petinside.global.exception.DuplicateFieldException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

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
}
