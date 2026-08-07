package org.example.petinside.domain.comment.repository;

import org.example.petinside.config.JpaAuditingConfig;
import org.example.petinside.domain.comment.entity.Comment;
import org.example.petinside.domain.post.entity.Category;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .username("user")
                .password("encoded")
                .nickname("user-nick")
                .role("USER")
                .build());

        post = postRepository.save(Post.builder()
                .author(user)
                .category(Category.QNA)
                .title("title")
                .content("content")
                .build());
    }

    private Comment save(Post post, Comment parent, String content, boolean deleted) {
        Comment comment = commentRepository.save(Comment.builder()
                .post(post)
                .user(user)
                .parent(parent)
                .content(content)
                .build());
        if (deleted) {
            comment.delete();
        }
        return comment;
    }

    @Test
    @DisplayName("최상위 댓글만 id 오름차순으로 조회하고, 삭제된 댓글은 제외한다")
    void findTopLevelComments_excludesDeleted_orderedById() {
        Comment first = save(post, null, "첫번째", false);
        save(post, null, "삭제된댓글", true);
        Comment second = save(post, null, "두번째", false);
        save(post, first, "대댓글은 제외", false);

        List<Comment> result = commentRepository
                .findAllByPostIdAndParentIsNullAndIsDeletedFalseOrderByIdAsc(post.getId());

        assertThat(result).extracting(Comment::getId).containsExactly(first.getId(), second.getId());
    }

    @Test
    @DisplayName("다른 게시글의 댓글은 포함하지 않는다")
    void findTopLevelComments_excludesOtherPosts() {
        Post otherPost = postRepository.save(Post.builder()
                .author(user)
                .category(Category.QNA)
                .title("다른 글")
                .content("content")
                .build());
        save(post, null, "이 글 댓글", false);
        save(otherPost, null, "다른 글 댓글", false);

        List<Comment> result = commentRepository
                .findAllByPostIdAndParentIsNullAndIsDeletedFalseOrderByIdAsc(post.getId());

        assertThat(result).extracting(Comment::getContent).containsExactly("이 글 댓글");
    }

    @Test
    @DisplayName("findByPostIdAndIsDeletedFalse는 대댓글을 포함해 삭제되지 않은 모든 댓글을 반환한다")
    void findByPostIdAndIsDeletedFalse_includesReplies_excludesDeleted() {
        Comment parent = save(post, null, "부모", false);
        Comment child = save(post, parent, "자식", false);
        save(post, parent, "삭제된 자식", true);

        List<Comment> result = commentRepository.findByPostIdAndIsDeletedFalse(post.getId());

        assertThat(result).extracting(Comment::getId).containsExactlyInAnyOrder(parent.getId(), child.getId());
    }
}
