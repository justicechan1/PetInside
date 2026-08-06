package org.example.petinside.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.comment.dto.CommentCreateRequest;
import org.example.petinside.domain.comment.dto.CommentResponse;
import org.example.petinside.domain.comment.entity.Comment;
import org.example.petinside.domain.comment.repository.CommentRepository;
import org.example.petinside.domain.post.dto.IdResponse;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
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

    /**
     * 댓글 및 대댓글 작성
     */
    @Transactional
    public IdResponse createComment(Long userId, Long postId, CommentCreateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다. id=" + userId));


        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 게시글입니다. id=" + postId));

        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .filter(c -> !c.isDeleted())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부모 댓글입니다. id=" + request.getParentId()));
        }


        Comment comment = Comment.builder()
                .content(request.getContent())
                .post(post)
                .user(user)
                .parent(parentComment)
                .build();

        Comment savedComment = commentRepository.save(comment);

        return new IdResponse(savedComment.getId());
    }

    /**
     * 특정 게시글의 댓글 목록 조회
     */
    public List<CommentResponse> getCommentsByPostId(Long postId) {
        // 게시글 존재 검증
        if (!postRepository.existsById(postId)) {
            throw new IllegalArgumentException("존재하지 않는 게시글입니다. id=" + postId);
        }

        List<Comment> parentComments = commentRepository.findAllByPostIdAndParentIsNullAndIsDeletedFalseOrderByIdAsc(postId);

        return parentComments.stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getContent(),
                        comment.getUser().getNickname(),
                        comment.getCreatedAt(),
                        // 대댓글 목록 매핑
                        comment.getChildren().stream()
                                .filter(child -> !child.isDeleted())
                                .map(child -> new CommentResponse(
                                        child.getId(),
                                        child.getContent(),
                                        child.getUser().getNickname(),
                                        child.getCreatedAt(),
                                        null
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    /**
     * 댓글 삭제 (Soft Delete)
     */
    @Transactional
    public IdResponse deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 이미 삭제된 댓글입니다. id=" + commentId));

        // 작성자 본인 확인
        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 댓글의 삭제 권한이 없습니다.");
        }

        comment.delete();

        return new IdResponse(comment.getId());
    }
}