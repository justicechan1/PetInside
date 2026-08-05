package org.example.petinside.mypage.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.common.exception.CustomException;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.entity.PostImage;
import org.example.petinside.domain.user.entity.SocialAccount;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.mypage.dto.MyPostResponse;
import org.example.petinside.mypage.dto.NicknameUpdateRequest;
import org.example.petinside.mypage.dto.PasswordUpdateRequest;
import org.example.petinside.mypage.dto.UserInfoResponse;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.domain.user.repository.SocialAccountRepository;
import org.example.petinside.domain.post.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MypageService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserInfoResponse getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "유저 없음"));

        String provider = socialAccountRepository.findByUserId(userId)
                .map(SocialAccount::getProvider)
                .orElse(null);

        return new UserInfoResponse(
                user.getId(), user.getUsername(), user.getNickname(),
                user.getProfileImageUrl(), user.getRole(), user.getCreatedAt(),
                provider
        );
    }

    @Transactional
    public void updateNickname(Long userId, NicknameUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404, "유저 없음"));
        user.updateNickname(request.getNickname());
    }

    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(404,"유저 없음"));

        if (user.getPassword() == null) {
            throw new CustomException(403, "소셜 로그인 사용자는 비밀번호 변경 불가");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException(401,"비밀번호 불일치");
        }
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    public Page<MyPostResponse> getMyPosts(Long userId, Pageable pageable) {
        return postRepository.findByAuthorIdAndIsDeletedFalse(userId, pageable)
                .map(post -> {
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
