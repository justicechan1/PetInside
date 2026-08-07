package org.example.petinside.domain.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.petinside.config.SecurityConfig;
import org.example.petinside.domain.comment.dto.CommentResponse;
import org.example.petinside.domain.comment.service.CommentService;
import org.example.petinside.domain.post.dto.IdResponse;
import org.example.petinside.global.exception.CommentNotFoundException;
import org.example.petinside.global.exception.CustomException;
import org.example.petinside.global.exception.PostNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

// CommentController 슬라이스 테스트 — 02_api_spec.md 3.1~3.4 응답 코드/형식 검증. CommentService는 Mock.
@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private CommentService commentService;

    // @AuthenticationPrincipal Long userId 를 채우기 위한 가짜 인증 (JWT 필터 미완성이라 직접 세팅)
    private RequestPostProcessor asUser(Long userId) {
        return authentication(new UsernamePasswordAuthenticationToken(userId, null));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId}/comments - 200")
    void getComments_ok() throws Exception {
        CommentResponse child = new CommentResponse(2L, "답글", "고양이집사", LocalDateTime.now(), List.of());
        CommentResponse parent = new CommentResponse(1L, "댓글", "강아지박사", LocalDateTime.now(), List.of(child));
        when(commentService.getCommentsByPostId(10L)).thenReturn(List.of(parent));

        mockMvc.perform(get("/api/v1/posts/{postId}/comments", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].children[0].id").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/posts/{postId}/comments - 없는 게시글이면 404")
    void getComments_postNotFound() throws Exception {
        when(commentService.getCommentsByPostId(999L)).thenThrow(new PostNotFoundException(999L));

        mockMvc.perform(get("/api/v1/posts/{postId}/comments", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/posts/{postId}/comments - 201")
    void createComment_ok() throws Exception {
        when(commentService.createComment(eq(1L), eq(10L), any())).thenReturn(new IdResponse(100L));

        String body = """
                {"content":"오리젠 추천합니다!","parentId":null}
                """;

        mockMvc.perform(post("/api/v1/posts/{postId}/comments", 10L)
                        .with(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    @DisplayName("POST /api/v1/posts/{postId}/comments - 내용이 비어있으면 400")
    void createComment_blankContent_badRequest() throws Exception {
        String body = """
                {"content":"","parentId":null}
                """;

        mockMvc.perform(post("/api/v1/posts/{postId}/comments", 10L)
                        .with(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/comments/{id} - 200")
    void updateComment_ok() throws Exception {
        when(commentService.updateComment(eq(1L), eq(1L), any())).thenReturn(new IdResponse(1L));

        String body = """
                {"content":"수정된 내용입니다."}
                """;

        mockMvc.perform(put("/api/v1/comments/{id}", 1L)
                        .with(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/comments/{id} - 내용이 비어있으면 400")
    void updateComment_blankContent_badRequest() throws Exception {
        String body = """
                {"content":""}
                """;

        mockMvc.perform(put("/api/v1/comments/{id}", 1L)
                        .with(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/comments/{id} - 작성자가 아니면 403")
    void updateComment_forbidden() throws Exception {
        when(commentService.updateComment(eq(2L), eq(1L), any()))
                .thenThrow(new CustomException(403, "해당 댓글의 수정 권한이 없습니다."));

        String body = """
                {"content":"수정된 내용입니다."}
                """;

        mockMvc.perform(put("/api/v1/comments/{id}", 1L)
                        .with(asUser(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/comments/{id} - 없는 댓글이면 404")
    void updateComment_notFound() throws Exception {
        when(commentService.updateComment(eq(1L), eq(999L), any()))
                .thenThrow(new CommentNotFoundException(999L));

        String body = """
                {"content":"수정된 내용입니다."}
                """;

        mockMvc.perform(put("/api/v1/comments/{id}", 999L)
                        .with(asUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/comments/{id} - 작성자가 아니면 403")
    void deleteComment_forbidden() throws Exception {
        when(commentService.deleteComment(2L, 1L))
                .thenThrow(new CustomException(403, "해당 댓글의 삭제 권한이 없습니다."));

        mockMvc.perform(delete("/api/v1/comments/{id}", 1L).with(asUser(2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/comments/{id} - 없는 댓글이면 404")
    void deleteComment_notFound() throws Exception {
        when(commentService.deleteComment(1L, 999L)).thenThrow(new CommentNotFoundException(999L));

        mockMvc.perform(delete("/api/v1/comments/{id}", 999L).with(asUser(1L)))
                .andExpect(status().isNotFound());
    }
}
