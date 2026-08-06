package org.example.petinside.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.example.petinside.domain.comment.repository.CommentRepository;
import org.example.petinside.domain.post.dto.*;
import org.example.petinside.domain.post.entity.Category;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.post.entity.PostImage;
import org.example.petinside.domain.post.repository.PostImageRepository;
import org.example.petinside.domain.post.repository.PostRepository;
import org.example.petinside.domain.user.entity.User;
import org.example.petinside.domain.user.repository.UserRepository;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.PostNotFoundException;
import org.example.petinside.global.exception.UserNotFoundException;
import org.example.petinside.global.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 생성
     */
    @Transactional
    public IdResponse createPost(Long userId, PostCreateRequest request) {
        // 작성자 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Post 엔티티 생성
        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(parseCategory(request.getCategory()))
                .author(user)
                .build();

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (String url : request.getImageUrls()) {
                PostImage image = PostImage.builder()
                        .imageUrl(url)
                        .build();
                post.addImage(image);
            }
        }

        Post savedPost = postRepository.save(post);

        return new IdResponse(savedPost.getId());
    }

    /**
     * 게시글 목록 조회 (최신순, Deleted 되지 않은 글, 카테고리/키워드 검색 및 페이징)
     */
    public PageResponse<PostListResponse> getPostList(String category, String keyword, Pageable pageable) {
        Category categoryFilter = (category != null && !category.isBlank())
                ? parseCategory(category)
                : null;

        Page<Post> posts = postRepository.search(categoryFilter, keyword, pageable);

        Page<PostListResponse> responsePage = posts.map(post -> {
            String thumbnailUrl = (post.getImages() != null && !post.getImages().isEmpty())
                    ? post.getImages().get(0).getImageUrl()
                    : null;

            int commentCount = (post.getComments() != null)
                    ? (int) post.getComments().stream().filter(c -> !c.isDeleted()).count()
                    : 0;

            return new PostListResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getCategory().name(),
                    post.getAuthor().getNickname(),
                    post.getViewCount(),
                    commentCount,
                    thumbnailUrl,
                    post.getCreatedAt()
            );
        });

        return PageResponse.from(responsePage);
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public PostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));

        post.increaseViewCount();

        List<String> imageUrls = post.getImages().stream()
                .map(PostImage::getImageUrl)
                .collect(Collectors.toList());

        return PostDetailResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory().name())
                .viewCount(post.getViewCount())
                .authorNickname(post.getAuthor().getNickname())
                .imageUrls(imageUrls)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     게시글 수정
     */
    @Transactional
    public IdResponse updatePost(Long userId, Long postId, PostUpdateRequest request) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));

        validateWriter(userId, post);

        post.update(
                parseCategory(request.getCategory()),
                request.getTitle(),
                request.getContent()
        );

        return new IdResponse(post.getId());
    }

    /**
     * 게시글 삭제 (Soft Delete)
     */
    @Transactional
    public IdResponse deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.isDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));

        validateWriterOrAdmin(userId, post);

        post.delete();

        return new IdResponse(post.getId());
    }

    // 잘못된 카테고리 문자열은 400으로 응답 (mypage 도메인과 동일한 컨벤션)
    private Category parseCategory(String category) {
        try {
            return Category.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(400, "유효하지 않은 카테고리입니다 (QNA, BOAST)");
        }
    }

    // 작성자 권한 검증
    private void validateWriter(Long userId, Post post) {
        if (!post.getAuthor().getId().equals(userId)) {
            throw new CustomException(403, "해당 게시글의 수정 권한이 없습니다.");
        }
    }

    // 작성자 또는 관리자 권한 검증
    private void validateWriterOrAdmin(Long userId, Post post) {
        if (post.getAuthor().getId().equals(userId)) {
            return;
        }

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!"ADMIN".equals(requester.getRole())) {
            throw new CustomException(403, "해당 게시글의 삭제 권한이 없습니다.");
        }
    }
}