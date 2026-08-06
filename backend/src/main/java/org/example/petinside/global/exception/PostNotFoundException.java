package org.example.petinside.global.exception;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(Long postId) {
        super("존재하지 않는 게시글입니다. id=" + postId);
    }
}
