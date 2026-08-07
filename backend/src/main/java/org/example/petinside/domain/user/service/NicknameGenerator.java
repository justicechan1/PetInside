package org.example.petinside.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NicknameGenerator {

    private static final String PREFIX = "user";

    private final UserRepository userRepository;

    // 소셜 로그인 최초 가입 시 임시 닉네임을 무작위로 생성 (닉네임 형식 \S{2,10} 준수, 마이페이지에서 변경 가능)
    public String generate() {
        String nickname;
        do {
            nickname = PREFIX + UUID.randomUUID().toString().substring(0, 6);
        } while (userRepository.existsByNickname(nickname));
        return nickname;
    }
}
