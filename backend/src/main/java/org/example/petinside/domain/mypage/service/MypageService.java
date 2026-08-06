package org.example.petinside.domain.mypage.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.domain.post.entity.Category;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.entity.PostImage;
import org.example.petinside.domain.user.entity.SocialAccount;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.mypage.dto.MyPostResponse;
import org.example.petinside.domain.mypage.dto.NicknameUpdateRequest;
import org.example.petinside.domain.mypage.dto.PasswordUpdateRequest;
import org.example.petinside.domain.mypage.dto.ProfileImageUpdateRequest;
import org.example.petinside.domain.mypage.dto.UserInfoResponse;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.domain.user.repository.SocialAccountRepository;
import org.example.petinside.domain.post.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 마이페이지 비즈니스 로직 - 내 정보 조회/수정, 게시글 목록 처리
@Service
@RequiredArgsConstructor
public class MypageService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;

    // 내 정보 조회 - User + SocialAccount 두 테이블을 조회해 소셜 로그인 여부 포함
    public UserInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "유저 없음"));

        // 소셜 계정이 없으면 null (일반 로그인 사용자)
        String provider = socialAccountRepository.findByUserId(userId)
                .map(SocialAccount::getProvider)
                .orElse(null);

        return new UserInfoResponse(
                user.getId(), user.getUsername(), user.getNickname(),
                user.getProfileImageUrl(), user.getRole(), user.getCreatedAt(),
                provider
        );
    }

    // [F-05] 닉네임 변경 - 중복 체크를 먼저 해서 불필요한 DB 조회 방지
    @Transactional
    public void updateNickname(Long userId, NicknameUpdateRequest request) {
        // 자신을 제외한 다른 유저가 동일 닉네임을 쓰고 있으면 409 반환
        if (userRepository.existsByNicknameAndIdNot(request.getNickname(), userId)) {
            throw new CustomException(409, "이미 사용중인 닉네임입니다");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "유저 없음"));
        // @Transactional + dirty checking으로 save() 없이 자동 반영
        user.updateNickname(request.getNickname());
    }

    // [F-06] 비밀번호 변경 - 소셜 로그인 사용자 차단 후 기존 비밀번호 검증
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "유저 없음"));

        // 소셜 로그인 사용자는 password가 null → 비밀번호 변경 불가
        if (user.getPassword() == null) {
            throw new CustomException(403, "소셜 로그인 사용자는 비밀번호 변경 불가");
        }

        // BCrypt로 암호화된 기존 비밀번호와 입력값 비교
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(401, "비밀번호 불일치");
        }
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    // [F-07] 프로필 사진 변경 - 클라이언트에서 받은 URL을 USER 테이블에 저장
    @Transactional
    public void updateProfileImage(Long userId, ProfileImageUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "유저 없음"));
        user.updateProfileImageUrl(request.getImageUrl());
    }

    // [F-08] 내 게시글 목록 조회 - 키워드/카테고리 선택적 필터링, 최신순 정렬
    public Page<MyPostResponse> getMyPosts(Long userId, String keyword, String category, Pageable pageable) {
        // 문자열 category를 enum으로 변환 (잘못된 값이면 400 반환)
        Category categoryEnum = null;
        if (category != null && !category.isBlank()) {
            try {
                categoryEnum = Category.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CustomException(400, "유효하지 않은 카테고리입니다 (QNA, BOAST)");
            }
        }

        // 빈 문자열도 null로 처리해 전체 조회와 동일하게 동작
        String keywordParam = (keyword != null && !keyword.isBlank()) ? keyword : null;

        return postRepository.searchMyPosts(userId, keywordParam, categoryEnum, pageable)
                .map(post -> {
                    // sortOrder가 0인 이미지를 썸네일로 사용
                    String thumbnail = post.getImages().stream()
                            .filter(img -> img.getSortOrder() == 0)
                            .map(PostImage::getImageUrl)
                            .findFirst()
                            .orElse(null);
                    return new MyPostResponse(
                            post.getId(), post.getCategory().name(), post.getTitle(),
                            thumbnail, post.getCreatedAt()
                    );
                });
    }
}
