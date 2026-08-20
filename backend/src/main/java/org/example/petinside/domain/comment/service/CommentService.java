package org.example.petinside.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.comment.dto.CommentCreateRequest;
import org.example.petinside.domain.comment.dto.CommentResponse;
import org.example.petinside.domain.comment.dto.CommentUpdateRequest;
import org.example.petinside.domain.comment.entity.Comment;
import org.example.petinside.domain.comment.repository.CommentRepository;
import org.example.petinside.domain.notification.entity.NotificationType;
import org.example.petinside.domain.notification.service.NotificationService;
import org.example.petinside.domain.post.dto.IdResponse;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CommentNotFoundException;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.PostNotFoundException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 댓글 및 대댓글 작성
     */
    @Transactional
    public IdResponse createComment(Long userId, Long postId, CommentCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));


        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));

        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> new CommentNotFoundException(request.getParentId()));

            // 대댓글에 다시 답글을 다는 3단계 중첩 방지 (목록 조회가 2단계까지만 응답에 포함시킴)
            if (parentComment.getParent() != null) {
                throw new CustomException(400, "대댓글에는 답글을 작성할 수 없습니다.");
            }
        }


        Comment comment = Comment.builder()
                .content(request.getContent())
                .post(post)
                .user(user)
                .parent(parentComment)
                .build();

        Comment savedComment = commentRepository.save(comment);

        // ===== 알림 생성 로직 추가 =====
        if (parentComment != null) {
            // 대댓글인 경우 → 부모 댓글 작성자에게 알림
            Long parentAuthorId = parentComment.getUser().getId();
            if (!parentAuthorId.equals(userId)) {  // 본인 댓글에 본인이 답글 단 경우 제외
                notificationService.createNotification(
                        parentAuthorId,
                        NotificationType.REPLY,
                        "회원님의 댓글에 답글이 달렸습니다.",
                        post.getId(),
                        "/posts/" + post.getId()
                );
            }
        } else {
            // 일반 댓글인 경우 → 게시글 작성자에게 알림
            Long postAuthorId = post.getAuthor().getId();
            if (!postAuthorId.equals(userId)) {  // 본인 글에 본인이 댓글 단 경우 제외
                notificationService.createNotification(
                        postAuthorId,
                        NotificationType.COMMENT,
                        "회원님의 게시글에 댓글이 달렸습니다.",
                        post.getId(),
                        "/posts/" + post.getId()
                );
            }
        }
        // ===== 알림 생성 로직 끝 =====

        return new IdResponse(savedComment.getId());
    }

    /**
     * 특정 게시글의 댓글 목록 조회
     */
    public List<CommentResponse> getCommentsByPostId(Long postId) {
        // 게시글 존재 검증
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }

        List<Comment> parentComments = commentRepository.findAllByPostIdAndParentIsNullAndIsDeletedFalseOrderByIdAsc(postId);

        return parentComments.stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getContent(),
                        comment.getUser().getId(),
                        comment.getUser().getNickname(),
                        comment.getUser().getProfileImageUrl(),
                        comment.getCreatedAt(),
                        // 대댓글 목록 매핑
                        comment.getChildren().stream()
                                .filter(child -> !child.isDeleted())
                                .map(child -> new CommentResponse(
                                        child.getId(),
                                        child.getContent(),
                                        child.getUser().getId(),
                                        child.getUser().getNickname(),
                                        child.getUser().getProfileImageUrl(),
                                        child.getCreatedAt(),
                                        null
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    /**
     * 댓글 및 대댓글 수정
     */
    @Transactional
    public IdResponse updateComment(Long userId, Long commentId, CommentUpdateRequest request) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        // 작성자 본인 확인
        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(403, "해당 댓글의 수정 권한이 없습니다.");
        }

        comment.update(request.getContent());

        return new IdResponse(comment.getId());
    }

    /**
     * 댓글 삭제 (Soft Delete)
     */
    @Transactional
    public IdResponse deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        // 작성자 본인 확인
        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(403, "해당 댓글의 삭제 권한이 없습니다.");
        }

        comment.delete();

        return new IdResponse(comment.getId());
    }
}