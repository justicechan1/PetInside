package org.example.petinside.domain.post.service;

import org.example.petinside.domain.comment.entity.Comment;
import org.example.petinside.domain.comment.repository.CommentRepository;
import org.example.petinside.domain.emoji.entity.Emoji;
import org.example.petinside.domain.emoji.entity.PostEmoji;
import org.example.petinside.domain.emoji.service.EmojiService;
import org.example.petinside.domain.post.dto.IdResponse;
import org.example.petinside.domain.post.dto.PostCreateRequest;
import org.example.petinside.domain.post.dto.PostDetailResponse;
import org.example.petinside.domain.post.dto.PostListResponse;
import org.example.petinside.domain.post.dto.PostUpdateRequest;
import org.example.petinside.domain.post.entity.Category;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.entity.PostImage;
import org.example.petinside.domain.post.repository.PostImageRepository;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.domain.subscription.repository.SubscriptionRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.PostNotFoundException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.example.petinside.global.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostImageRepository postImageRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmojiService emojiService;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private PostService postService;

    private User author;

    @BeforeEach
    void setUp() {
        postService = new PostService(postRepository, postImageRepository, commentRepository, userRepository, emojiService, subscriptionRepository);

        lenient().when(subscriptionRepository.findUserIdsByUserIdInAndStatus(any(), any())).thenReturn(java.util.Set.of());

        author = User.builder()
                .username("author")
                .password("encoded")
                .nickname("author-nick")
                .role("USER")
                .build();
        ReflectionTestUtils.setField(author, "id", 1L);
        author.updateProfileImageUrl("http://profile/author.png");

        lenient().when(emojiService.resolveEmojisForAttach(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
    }

    private PostCreateRequest createRequest(String category, String title, String content, List<String> imageUrls) {
        return createRequest(category, title, content, imageUrls, null);
    }

    private PostCreateRequest createRequest(String category, String title, String content, List<String> imageUrls, List<Long> emojiIds) {
        PostCreateRequest request = new PostCreateRequest();
        ReflectionTestUtils.setField(request, "category", category);
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "content", content);
        ReflectionTestUtils.setField(request, "imageUrls", imageUrls);
        ReflectionTestUtils.setField(request, "emojiIds", emojiIds);
        return request;
    }

    private PostUpdateRequest updateRequest(String category, String title, String content) {
        PostUpdateRequest request = new PostUpdateRequest();
        ReflectionTestUtils.setField(request, "category", category);
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }

    private Post buildPost(Long id, User author, Category category, boolean deleted) {
        Post post = Post.builder()
                .author(author)
                .category(category)
                .title("title")
                .content("content")
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        if (deleted) {
            post.delete();
        }
        return post;
    }

    @Nested
    @DisplayName("createPost")
    class CreatePost {

        @Test
        @DisplayName("유효한 요청이면 게시글을 저장하고 id를 반환한다")
        void createPost_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post post = invocation.getArgument(0);
                ReflectionTestUtils.setField(post, "id", 100L);
                return post;
            });

            PostCreateRequest request = createRequest("qna", "제목", "내용", null);

            IdResponse response = postService.createPost(1L, request);

            assertThat(response.getId()).isEqualTo(100L);
            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            assertThat(captor.getValue().getCategory()).isEqualTo(Category.QNA);
            assertThat(captor.getValue().getAuthor()).isEqualTo(author);
            assertThat(captor.getValue().getImages()).isEmpty();
        }

        @Test
        @DisplayName("imageUrls가 있으면 이미지들을 게시글에 추가한다")
        void createPost_withImageUrls_addsImages() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(author));
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PostCreateRequest request = createRequest("boast", "제목", "내용",
                    List.of("http://img1", "http://img2"));

            postService.createPost(1L, request);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            List<PostImage> images = captor.getValue().getImages();
            assertThat(images).hasSize(2);
            assertThat(images).extracting(PostImage::getImageUrl)
                    .containsExactly("http://img1", "http://img2");
            assertThat(images).allSatisfy(image -> assertThat(image.getPost()).isEqualTo(captor.getValue()));
        }

        @Test
        @DisplayName("존재하지 않는 유저면 예외가 발생하고 저장하지 않는다")
        void createPost_userNotFound_throws() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            PostCreateRequest request = createRequest("qna", "제목", "내용", null);

            assertThatThrownBy(() -> postService.createPost(999L, request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("emojiIds가 있으면 EmojiService로 검증된 이모지를 요청 순서대로 게시글에 첨부한다")
        void createPost_withEmojiIds_addsEmojis() {
            Emoji smile = Emoji.builder().name("웃음").imageUrl("http://emoji/smile").build();
            Emoji heart = Emoji.builder().name("하트").imageUrl("http://emoji/heart").build();
            ReflectionTestUtils.setField(smile, "id", 1L);
            ReflectionTestUtils.setField(heart, "id", 2L);

            when(userRepository.findById(1L)).thenReturn(Optional.of(author));
            when(emojiService.resolveEmojisForAttach(1L, List.of(1L, 2L))).thenReturn(List.of(smile, heart));
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PostCreateRequest request = createRequest("qna", "제목", "내용", null, List.of(1L, 2L));

            postService.createPost(1L, request);

            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(captor.capture());
            List<PostEmoji> emojis = captor.getValue().getEmojis();
            assertThat(emojis).hasSize(2);
            assertThat(emojis).extracting(e -> e.getEmoji().getId()).containsExactly(1L, 2L);
        }
    }

    @Nested
    @DisplayName("getPostList")
    class GetPostList {

        @Test
        @DisplayName("카테고리가 공백이면 카테고리 필터 없이 검색한다")
        void getPostList_blankCategory_passesNullFilter() {
            Pageable pageable = PageRequest.of(0, 10);
            when(postRepository.search(isNull(), eq("k"), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of()));

            postService.getPostList("  ", "k", pageable);

            verify(postRepository).search(isNull(), eq("k"), eq(pageable));
        }

        @Test
        @DisplayName("게시글 목록을 썸네일/댓글수/작성자닉네임 포함하여 매핑한다")
        void getPostList_mapsThumbnailAndCommentCount() {
            Post post = buildPost(1L, author, Category.QNA, false);
            PostImage image = PostImage.builder().imageUrl("http://thumb").sortOrder(0).build();
            post.addImage(image);

            Comment activeComment = Comment.builder().post(post).user(author).content("c1").build();
            Comment deletedComment = Comment.builder().post(post).user(author).content("c2").build();
            deletedComment.delete();
            ReflectionTestUtils.setField(post, "comments", List.of(activeComment, deletedComment));

            Pageable pageable = PageRequest.of(0, 10);
            when(postRepository.search(eq(Category.QNA), isNull(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(post)));

            PageResponse<PostListResponse> result = postService.getPostList("qna", null, pageable);

            assertThat(result.content()).hasSize(1);
            PostListResponse dto = result.content().get(0);
            assertThat(dto.getThumbnailUrl()).isEqualTo("http://thumb");
            assertThat(dto.getCommentCount()).isEqualTo(1);
            assertThat(dto.getAuthorId()).isEqualTo(1L);
            assertThat(dto.getAuthorNickname()).isEqualTo("author-nick");
            assertThat(dto.getAuthorProfileImageUrl()).isEqualTo("http://profile/author.png");
            assertThat(dto.isAuthorVerified()).isFalse();
        }

        @Test
        @DisplayName("작성자가 활성 구독자면 인증 뱃지가 true다")
        void getPostList_verifiedAuthor_setsAuthorVerifiedTrue() {
            Post post = buildPost(1L, author, Category.QNA, false);
            Pageable pageable = PageRequest.of(0, 10);
            when(postRepository.search(any(), any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(post)));
            when(subscriptionRepository.findUserIdsByUserIdInAndStatus(List.of(1L), org.example.petinside.domain.subscription.entity.SubscriptionStatus.ACTIVE))
                    .thenReturn(java.util.Set.of(1L));

            PageResponse<PostListResponse> result = postService.getPostList(null, null, pageable);

            assertThat(result.content().get(0).isAuthorVerified()).isTrue();
        }

        @Test
        @DisplayName("이미지가 없는 게시글은 썸네일이 null이다")
        void getPostList_noImages_thumbnailIsNull() {
            Post post = buildPost(1L, author, Category.QNA, false);
            Pageable pageable = PageRequest.of(0, 10);
            when(postRepository.search(any(), any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(post)));

            PageResponse<PostListResponse> result = postService.getPostList(null, null, pageable);

            assertThat(result.content().get(0).getThumbnailUrl()).isNull();
            assertThat(result.content().get(0).getCommentCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getPostDetail")
    class GetPostDetail {

        @Test
        @DisplayName("조회에 성공하면 조회수를 증가시키고 상세 정보를 반환한다")
        void getPostDetail_success_increasesViewCount() {
            Post post = buildPost(1L, author, Category.QNA, false);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            PostDetailResponse response = postService.getPostDetail(1L);

            assertThat(post.getViewCount()).isEqualTo(1);
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getAuthorId()).isEqualTo(1L);
            assertThat(response.getAuthorNickname()).isEqualTo("author-nick");
            assertThat(response.getAuthorProfileImageUrl()).isEqualTo("http://profile/author.png");
            assertThat(response.isAuthorVerified()).isFalse();
        }

        @Test
        @DisplayName("작성자가 활성 구독자면 인증 뱃지가 true다")
        void getPostDetail_verifiedAuthor_setsAuthorVerifiedTrue() {
            Post post = buildPost(1L, author, Category.QNA, false);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(subscriptionRepository.existsByUser_IdAndStatus(1L, org.example.petinside.domain.subscription.entity.SubscriptionStatus.ACTIVE))
                    .thenReturn(true);

            PostDetailResponse response = postService.getPostDetail(1L);

            assertThat(response.isAuthorVerified()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 게시글이면 예외가 발생한다")
        void getPostDetail_notFound_throws() {
            when(postRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getPostDetail(1L))
                    .isInstanceOf(PostNotFoundException.class);
        }

        @Test
        @DisplayName("삭제된 게시글이면 예외가 발생한다")
        void getPostDetail_deleted_throws() {
            Post post = buildPost(1L, author, Category.QNA, true);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.getPostDetail(1L))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("작성자 본인이면 게시글을 수정한다")
        void updatePost_owner_success() {
            Post post = buildPost(1L, author, Category.QNA, false);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            IdResponse response = postService.updatePost(1L, 1L, updateRequest("boast", "수정제목", "수정내용"));

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(post.getCategory()).isEqualTo(Category.BOAST);
            assertThat(post.getTitle()).isEqualTo("수정제목");
            assertThat(post.getContent()).isEqualTo("수정내용");
        }

        @Test
        @DisplayName("emojiIds를 전달하면 기존 이모지를 전부 교체한다")
        void updatePost_withEmojiIds_replacesEmojis() {
            Post post = buildPost(1L, author, Category.QNA, false);
            Emoji oldEmoji = Emoji.builder().name("old").imageUrl("http://emoji/old").build();
            ReflectionTestUtils.setField(oldEmoji, "id", 9L);
            post.addEmoji(PostEmoji.builder().emoji(oldEmoji).sortOrder(0).build());

            Emoji newEmoji = Emoji.builder().name("new").imageUrl("http://emoji/new").build();
            ReflectionTestUtils.setField(newEmoji, "id", 3L);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(emojiService.resolveEmojisForAttach(1L, List.of(3L))).thenReturn(List.of(newEmoji));

            PostUpdateRequest request = updateRequest("qna", "t", "c");
            ReflectionTestUtils.setField(request, "emojiIds", List.of(3L));

            postService.updatePost(1L, 1L, request);

            assertThat(post.getEmojis()).hasSize(1);
            assertThat(post.getEmojis().get(0).getEmoji().getId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("작성자가 아니면 예외가 발생한다")
        void updatePost_notOwner_throws() {
            Post post = buildPost(1L, author, Category.QNA, false);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> postService.updatePost(2L, 1L, updateRequest("qna", "t", "c")))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("삭제되었거나 존재하지 않으면 예외가 발생한다")
        void updatePost_deletedOrMissing_throws() {
            when(postRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.updatePost(1L, 1L, updateRequest("qna", "t", "c")))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deletePost")
    class DeletePost {

        @Test
        @DisplayName("작성자 본인이면 삭제(soft delete)한다")
        void deletePost_owner_success() {
            Post post = buildPost(1L, author, Category.QNA, false);
            when(postRepository.findById(1L)).thenReturn(Optional.of(post));

            IdResponse response = postService.deletePost(1L, 1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(post.isDeleted()).isTrue();
            verify(userRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("작성자가 아니어도 관리자면 삭제한다")
        void deletePost_admin_success() {
            Post post = buildPost(1L, author, Category.QNA, false);
            User admin = User.builder().username("admin").password("p").nickname("admin-nick").role("ADMIN").build();
            ReflectionTestUtils.setField(admin, "id", 2L);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

            IdResponse response = postService.deletePost(2L, 1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(post.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("작성자도 관리자도 아니면 예외가 발생한다")
        void deletePost_notOwnerNotAdmin_throws() {
            Post post = buildPost(1L, author, Category.QNA, false);
            User other = User.builder().username("other").password("p").nickname("other-nick").role("USER").build();
            ReflectionTestUtils.setField(other, "id", 2L);

            when(postRepository.findById(1L)).thenReturn(Optional.of(post));
            when(userRepository.findById(2L)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> postService.deletePost(2L, 1L))
                    .isInstanceOf(CustomException.class);
            assertThat(post.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("이미 삭제되었거나 존재하지 않으면 예외가 발생한다")
        void deletePost_deletedOrMissing_throws() {
            when(postRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.deletePost(1L, 1L))
                    .isInstanceOf(PostNotFoundException.class);
        }
    }
}
