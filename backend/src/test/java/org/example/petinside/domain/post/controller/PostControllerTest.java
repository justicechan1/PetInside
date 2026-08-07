package org.example.petinside.domain.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.petinside.config.SecurityConfig;
import org.example.petinside.domain.post.dto.IdResponse;
import org.example.petinside.domain.post.dto.PostDetailResponse;
import org.example.petinside.domain.post.dto.PostListResponse;
import org.example.petinside.domain.post.service.PostService;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.PostNotFoundException;
import org.example.petinside.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PostController 슬라이스 테스트 — 02_api_spec.md 2.1~2.5 응답 코드/형식 검증. PostService는 Mock.
@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private PostService postService;

    // @AuthenticationPrincipal Long userId 를 채우기 위한 가짜 인증 (JWT 필터 미완성이라 직접 세팅)
    private RequestPostProcessor asUser(Long userId) {
        return authentication(new UsernamePasswordAuthenticationToken(userId, null));
    }

    @Test
    @DisplayName("GET /api/v1/posts - 200")
    void getPostList_ok() throws Exception {
        PostListResponse item = new PostListResponse(1L, "제목", "QNA", "닉네임", 0, 0, null, LocalDateTime.now());
        PageResponse<PostListResponse> page = new PageResponse<>(List.of(item), 0, 10, 1, 1);
        when(postService.getPostList(eq("qna"), eq("사료"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/posts").param("category", "qna").param("keyword", "사료"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.pageNumber").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{id} - 200")
    void getPostDetail_ok() throws Exception {
        PostDetailResponse detail = PostDetailResponse.builder()
                .id(1L).title("제목").content("내용").category("QNA")
                .viewCount(1).authorNickname("닉네임").imageUrls(List.of())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(postService.getPostDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/posts/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("제목"));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{id} - 없는 게시글이면 404")
    void getPostDetail_notFound() throws Exception {
        when(postService.getPostDetail(999L)).thenThrow(new PostNotFoundException(999L));

        mockMvc.perform(get("/api/v1/posts/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/posts - 201")
    void createPost_ok() throws Exception {
        when(postService.createPost(eq(1L), any())).thenReturn(new IdResponse(10L));

        String body = """
                {"category":"QNA","title":"제목","content":"내용"}
                """;

        mockMvc.perform(post("/api/v1/posts")
                        .with(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    @DisplayName("POST /api/v1/posts - 필수값 누락이면 400")
    void createPost_blankTitle_badRequest() throws Exception {
        String body = """
                {"category":"QNA","title":"","content":"내용"}
                """;

        mockMvc.perform(post("/api/v1/posts")
                        .with(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/posts/{id} - 작성자가 아니면 403")
    void updatePost_forbidden() throws Exception {
        when(postService.updatePost(eq(2L), eq(1L), any()))
                .thenThrow(new CustomException(403, "해당 게시글의 수정 권한이 없습니다."));

        String body = """
                {"category":"QNA","title":"수정","content":"수정내용"}
                """;

        mockMvc.perform(put("/api/v1/posts/{id}", 1L)
                        .with(asUser(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("DELETE /api/v1/posts/{id} - 200")
    void deletePost_ok() throws Exception {
        when(postService.deletePost(eq(1L), eq(1L))).thenReturn(new IdResponse(1L));

        mockMvc.perform(delete("/api/v1/posts/{id}", 1L).with(asUser(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }
}
