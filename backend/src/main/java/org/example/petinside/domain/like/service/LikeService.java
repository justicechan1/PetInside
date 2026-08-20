package org.example.petinside.domain.like.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.comment.entity.Comment;
import org.example.petinside.domain.comment.repository.CommentRepository;
import org.example.petinside.domain.like.dto.LikeResponse;
import org.example.petinside.domain.like.entity.CommentLike;
import org.example.petinside.domain.like.entity.PostLike;
import org.example.petinside.domain.like.repository.CommentLikeRepository;
import org.example.petinside.domain.like.repository.PostLikeRepository;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CommentNotFoundException;
import org.example.petinside.global.exception.DuplicateFieldException;
import org.example.petinside.global.exception.PostNotFoundException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

    private final PostLikeRepository postLikeRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 좋아요 토글 - 이미 눌렀으면 취소(-1), 안 눌렀으면 등록(+1). 작성자 본인도 가능.
     */
    @Transactional
    public LikeResponse togglePostLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));

        Optional<PostLike> existing = postLikeRepository.findByPostIdAndUserId(postId, userId);

        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.decreaseLikeCount();
            return new LikeResponse(false, post.getLikeCount());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        try {
            postLikeRepository.saveAndFlush(PostLike.of(post, user));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateFieldException("이미 좋아요를 누른 게시글입니다.");
        }

        post.increaseLikeCount();
        return new LikeResponse(true, post.getLikeCount());
    }

    /**
     * 게시글 좋아요 상태 조회 (내가 눌렀는지 여부 + 총 좋아요 수)
     */
    public LikeResponse getPostLikeStatus(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));

        boolean liked = userId != null && postLikeRepository.existsByPostIdAndUserId(postId, userId);
        return new LikeResponse(liked, post.getLikeCount());
    }

    /**
     * 댓글 좋아요 토글 - 이미 눌렀으면 취소(-1), 안 눌렀으면 등록(+1). 작성자 본인도 가능.
     */
    @Transactional
    public LikeResponse toggleCommentLike(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        Optional<CommentLike> existing = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);

        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
            comment.decreaseLikeCount();
            return new LikeResponse(false, comment.getLikeCount());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        try {
            commentLikeRepository.saveAndFlush(CommentLike.of(comment, user));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateFieldException("이미 좋아요를 누른 댓글입니다.");
        }

        comment.increaseLikeCount();
        return new LikeResponse(true, comment.getLikeCount());
    }

    /**
     * 댓글 좋아요 상태 조회 (내가 눌렀는지 여부 + 총 좋아요 수)
     */
    public LikeResponse getCommentLikeStatus(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        boolean liked = userId != null && commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
        return new LikeResponse(liked, comment.getLikeCount());
    }
}
