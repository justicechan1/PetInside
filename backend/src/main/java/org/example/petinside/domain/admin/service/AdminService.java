package org.example.petinside.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.admin.entity.Role;
import org.example.petinside.domain.admin.dto.*;
import org.example.petinside.domain.payment.dto.PaymentSummaryResponse;
import org.example.petinside.domain.payment.entity.Payment;
import org.example.petinside.domain.payment.entity.PaymentStatus;
import org.example.petinside.domain.payment.repository.PaymentRepository;
import org.example.petinside.domain.subscription.dto.SubscriptionSummaryResponse;
import org.example.petinside.domain.subscription.entity.Subscription;
import org.example.petinside.domain.subscription.entity.SubscriptionStatus;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.example.petinside.domain.user.dto.UserSummaryResponse;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.global.exception.CommentNotFoundException;
import org.example.petinside.global.exception.PostNotFoundException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.example.petinside.domain.comment.entity.Comment;
import org.example.petinside.domain.comment.repository.CommentRepository;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.domain.auth.repository.RefreshTokenRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


// admin/AdminService.java
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    // F-15: 권한 부여
    @Transactional
    public RoleUpdateResponse updateRole(Long userId, Role role) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.updateRole(userId, role.name());
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


    // F-40: 회원 삭제 처리
    @Transactional
    public DeleteResponse deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 회원의 게시글 soft 삭제
        Page<Post> posts = postRepository.findByAuthorIdAndIsDeletedFalse(userId, Pageable.unpaged());
        posts.forEach(Post::delete);

        //회원의 댓글 soft 삭제
        List<Comment> comments = commentRepository.findAllByAuthorIdAndIsDeletedFalse(userId);
        comments.forEach(Comment::delete);

        //회원 삭제
        user.delete();

        return new DeleteResponse(user.getId());
    }

    // F-41: 구독 회원 목록 조회
    @Transactional(readOnly = true)
    public Page<SubscriptionSummaryResponse> getSubscriptions(SubscriptionStatus status,
                                                              LocalDateTime startDate,
                                                              LocalDateTime endDate,
                                                              Pageable pageable
    ) {
        Page<Subscription> subscriptions;

        if (status != null) {
            subscriptions = subscriptionRepository.findByStatus(status, pageable);
        } else if (startDate != null && endDate != null) {
            subscriptions = subscriptionRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        } else {
            subscriptions = subscriptionRepository.findAll(pageable);
        }

        return subscriptions.map(SubscriptionSummaryResponse::from);
    }

    //F-42: 구독 회원 결제 내역 조회
    @Transactional(readOnly = true)
    public Page<PaymentSummaryResponse> getPayments(PaymentStatus status, Long userId, Pageable pageable) {
        Page<Payment> payments;

        if (status != null) {
            payments = paymentRepository.findByStatus(status, pageable);
        } else if (userId != null) {
            payments = paymentRepository.findByUserId(userId, pageable);
        } else {
            payments = paymentRepository.findAll(pageable);
        }

        return payments.map(PaymentSummaryResponse::from);
    }


}
