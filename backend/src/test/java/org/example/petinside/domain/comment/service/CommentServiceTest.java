package org.example.petinside.domain.comment.service;

import org.example.petinside.domain.comment.dto.CommentCreateRequest;
import org.example.petinside.domain.comment.dto.CommentResponse;
import org.example.petinside.domain.comment.entity.Comment;
import org.example.petinside.domain.comment.repository.CommentRepository;
import org.example.petinside.domain.post.dto.IdResponse;
import org.example.petinside.domain.post.entity.Category;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CommentNotFoundException;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.PostNotFoundException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;

    private CommentService commentService;

    private User author;
    private Post post;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, postRepository, userRepository);

        author = User.builder()
                .username("author")
                .password("encoded")
                .nickname("author-nick")
                .role("USER")
                .build();
        ReflectionTestUtils.setField(author, "id", 1L);

        post = Post.builder()
                .author(author)
                .category(Category.QNA)
                .title("title")
                .content("content")
                .build();
        ReflectionTestUtils.setField(post, "id", 10L);
    }

    private CommentCreateRequest createRequest(String content, Long parentId) {
        CommentCreateRequest request = new CommentCreateRequest();
        ReflectionTestUtils.setField(request, "content", content);
        ReflectionTestUtils.setField(request, "parentId", parentId);
        return request;
    }

    private Comment buildComment(Long id, Post post, User user, Comment parent, boolean deleted) {
        Comment comment = Comment.builder().post(post).user(user).parent(parent).content("내용").build();
        ReflectionTestUtils.setField(comment, "id", id);
        if (deleted) {
            comment.delete();
        }
        return comment;
    }

    @Nested
    @DisplayName("createComment")
    class CreateComment {

        @Test
        @DisplayName("최상위 댓글을 생성한다")
        void createComment_topLevel_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
                Comment c = invocation.getArgument(0);
                ReflectionTestUtils.setField(c, "id", 100L);
                return c;
            });

            IdResponse response = commentService.createComment(1L, 10L, createRequest("내용", null));

            assertThat(response.getId()).isEqualTo(100L);
            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertThat(captor.getValue().getParent()).isNull();
            assertThat(captor.getValue().getUser()).isEqualTo(author);
        }

        @Test
        @DisplayName("parentId가 있으면 대댓글로 생성한다")
        void createComment_reply_success() {
            Comment parent = buildComment(50L, post, author, null, false);

            when(userRepository.findById(1L)).thenReturn(Optional.of(author));
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(commentRepository.findById(50L)).thenReturn(Optional.of(parent));
            when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

            commentService.createComment(1L, 10L, createRequest("답글", 50L));

            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentRepository).save(captor.capture());
            assertThat(captor.getValue().getParent()).isEqualTo(parent);
        }

        @Test
        @DisplayName("존재하지 않는 유저면 예외가 발생한다")
        void createComment_userNotFound_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(1L, 10L, createRequest("내용", null)))
                    .isInstanceOf(UserNotFoundException.class);
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않거나 삭제된 게시글이면 예외가 발생한다")
        void createComment_postNotFoundOrDeleted_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));
            when(postRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(1L, 10L, createRequest("내용", null)))
                    .isInstanceOf(PostNotFoundException.class);
            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않거나 삭제된 부모 댓글이면 예외가 발생한다")
        void createComment_parentNotFoundOrDeleted_throws() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));
            when(postRepository.findById(10L)).thenReturn(Optional.of(post));
            when(commentRepository.findById(50L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.createComment(1L, 10L, createRequest("답글", 50L)))
                    .isInstanceOf(CommentNotFoundException.class);
            verify(commentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getCommentsByPostId")
    class GetCommentsByPostId {

        @Test
        @DisplayName("존재하지 않는 게시글이면 예외가 발생한다")
        void getComments_postNotFound_throws() {
            when(postRepository.existsById(10L)).thenReturn(false);

            assertThatThrownBy(() -> commentService.getCommentsByPostId(10L))
                    .isInstanceOf(PostNotFoundException.class);
        }

        @Test
        @DisplayName("최상위 댓글과, 삭제되지 않은 대댓글만 트리 형태로 반환한다")
        void getComments_returnsNestedTree_filteringDeletedChildren() {
            Comment parent = buildComment(1L, post, author, null, false);
            Comment activeChild = buildComment(2L, post, author, parent, false);
            Comment deletedChild = buildComment(3L, post, author, parent, true);
            ReflectionTestUtils.setField(parent, "children", List.of(activeChild, deletedChild));

            when(postRepository.existsById(10L)).thenReturn(true);
            when(commentRepository.findAllByPostIdAndParentIsNullAndIsDeletedFalseOrderByIdAsc(10L))
                    .thenReturn(List.of(parent));

            List<CommentResponse> result = commentService.getCommentsByPostId(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            assertThat(result.get(0).getChildren()).hasSize(1);
            assertThat(result.get(0).getChildren().get(0).getId()).isEqualTo(2L);
            assertThat(result.get(0).getChildren().get(0).getAuthorNickname()).isEqualTo("author-nick");
        }
    }

    @Nested
    @DisplayName("deleteComment")
    class DeleteComment {

        @Test
        @DisplayName("작성자 본인이면 삭제(soft delete)한다")
        void deleteComment_owner_success() {
            Comment comment = buildComment(1L, post, author, null, false);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            IdResponse response = commentService.deleteComment(1L, 1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(comment.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("작성자가 아니면 예외가 발생한다")
        void deleteComment_notOwner_throws() {
            Comment comment = buildComment(1L, post, author, null, false);
            when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

            assertThatThrownBy(() -> commentService.deleteComment(2L, 1L))
                    .isInstanceOf(CustomException.class);
            assertThat(comment.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("존재하지 않거나 이미 삭제된 댓글이면 예외가 발생한다")
        void deleteComment_notFoundOrAlreadyDeleted_throws() {
            when(commentRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentService.deleteComment(1L, 1L))
                    .isInstanceOf(CommentNotFoundException.class);
        }
    }
}
