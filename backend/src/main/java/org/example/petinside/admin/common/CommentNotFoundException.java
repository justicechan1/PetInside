package org.example.petinside.admin.common;

public class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException(Long commentId) {
        super("존재하지 않는 댓글입니다. id=" + commentId);
    }
}
