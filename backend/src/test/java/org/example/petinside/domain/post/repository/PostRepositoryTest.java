package org.example.petinside.domain.post.repository;

import jakarta.persistence.EntityManager;
import org.example.petinside.config.JpaAuditingConfig;
import org.example.petinside.domain.post.entity.Category;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.entity.PostImage;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    private User author;

    @BeforeEach
    void setUp() {
        author = userRepository.save(User.builder()
                .username("author")
                .password("encoded")
                .nickname("author-nick")
                .role("USER")
                .build());
    }

    private Post save(Category category, String title, boolean deleted) {
        Post post = Post.builder()
                .author(author)
                .category(category)
                .title(title)
                .content("content")
                .build();
        Post saved = postRepository.save(post);
        if (deleted) {
            saved.delete();
        }
        return saved;
    }

    @Test
    @DisplayName("삭제된 게시글은 검색 결과에서 제외된다")
    void search_excludesDeletedPosts() {
        save(Category.QNA, "살아있는 글", false);
        save(Category.QNA, "삭제된 글", true);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> result = postRepository.search(null, null, pageable);

        assertThat(result.getContent()).extracting(Post::getTitle).containsExactly("살아있는 글");
    }

    @Test
    @DisplayName("카테고리로 필터링한다")
    void search_filtersByCategory() {
        save(Category.QNA, "질문글", false);
        save(Category.BOAST, "자랑글", false);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> result = postRepository.search(Category.BOAST, null, pageable);

        assertThat(result.getContent()).extracting(Post::getTitle).containsExactly("자랑글");
    }

    @Test
    @DisplayName("제목 키워드로 필터링한다")
    void search_filtersByKeyword() {
        save(Category.QNA, "강아지 산책 질문", false);
        save(Category.QNA, "고양이 사료 질문", false);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> result = postRepository.search(null, "강아지", pageable);

        assertThat(result.getContent()).extracting(Post::getTitle).containsExactly("강아지 산책 질문");
    }

    @Test
    @DisplayName("카테고리와 키워드가 모두 없으면 삭제되지 않은 전체 글을 반환한다")
    void search_noFilters_returnsAllNonDeleted() {
        save(Category.QNA, "글1", false);
        save(Category.BOAST, "글2", false);
        Pageable pageable = PageRequest.of(0, 10);

        Page<Post> result = postRepository.search(null, null, pageable);

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Pageable에 sort=viewCount,desc를 넘기면 조회수 내림차순으로 정렬된다")
    void search_sortsByViewCountDescending_whenRequested() {
        Post low = save(Category.QNA, "조회수 낮음", false);
        Post high = save(Category.QNA, "조회수 높음", false);
        Post mid = save(Category.QNA, "조회수 중간", false);
        ReflectionTestUtils.setField(low, "viewCount", 1);
        ReflectionTestUtils.setField(high, "viewCount", 100);
        ReflectionTestUtils.setField(mid, "viewCount", 50);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "viewCount"));

        Page<Post> result = postRepository.search(null, null, pageable);

        assertThat(result.getContent()).extracting(Post::getTitle)
                .containsExactly("조회수 높음", "조회수 중간", "조회수 낮음");
    }

    @Test
    @DisplayName("이미지는 삽입 순서와 무관하게 sort_order 오름차순으로 조회된다 (F-37 썸네일 = images.get(0) 전제)")
    void images_areLoadedOrderedBySortOrder_regardlessOfInsertionOrder() {
        Post post = save(Category.QNA, "이미지 있는 글", false);
        post.addImage(PostImage.builder().imageUrl("http://second").sortOrder(1).build());
        post.addImage(PostImage.builder().imageUrl("http://first").sortOrder(0).build());
        postRepository.save(post);

        entityManager.flush();
        entityManager.clear();

        Post reloaded = postRepository.findById(post.getId()).orElseThrow();

        assertThat(reloaded.getImages()).extracting(PostImage::getImageUrl)
                .containsExactly("http://first", "http://second");
    }
}
