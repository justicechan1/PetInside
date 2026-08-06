package org.example.petinside.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.global.exception.CommentNotFoundException;
import org.example.petinside.global.exception.PostNotFoundException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.example.petinside.domain.comment.entity.Comment;
import org.example.petinside.domain.comment.repository.CommentRepository;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.admin.dto.DailyStatisticsResponse;
import org.example.petinside.admin.dto.DeleteResponse;
import org.example.petinside.admin.dto.RoleUpdateResponse;
import org.example.petinside.admin.dto.UserSummaryResponse;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.domain.auth.repository.RefreshTokenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;


// admin/AdminService.java
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // F-15: 권한 부여
    @Transactional
    public RoleUpdateResponse updateRole(Long userId, String role) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.updateRole(userId, role);
        return new RoleUpdateResponse(userId, role);
    }

    // F-16: 게시글 삭제 처리
    @Transactional
    public DeleteResponse deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));
        post.delete(); // is_deleted = true
        return new DeleteResponse(post.getId());
    }

    // F-17: 회원 목록 조회
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserSummaryResponse::from);
    }

    // F-18: 서비스 통계 조회
    @Transactional(readOnly = true)
    public DailyStatisticsResponse getDailyStatistics() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long newUserCount = userRepository.countByCreatedAtAfter(startOfDay);
        long newPostCount = postRepository.countByCreatedAtAfter(startOfDay);
        long activeUserCount = refreshTokenRepository.countDistinctUsersLoggedInSince(startOfDay);

        return new DailyStatisticsResponse(
                LocalDate.now(), newUserCount, newPostCount, activeUserCount
        );
    }

    // F-19: 댓글 삭제 처리
    @Transactional
    public DeleteResponse deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new CommentNotFoundException(commentId));
        comment.delete(); // is_deleted = true
        return new DeleteResponse(comment.getId());
    }



}
