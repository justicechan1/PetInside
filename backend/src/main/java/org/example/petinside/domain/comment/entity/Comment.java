package org.example.petinside.domain.comment.entity;

import org.example.petinside.domain.emoji.entity.CommentEmoji;
import org.example.petinside.domain.post.entity.Post;
import org.example.petinside.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User user;

    // 대댓글 셀프 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Comment> children = new ArrayList<>();

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CommentEmoji> emojis = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Comment(Post post, User user, Comment parent, String content) {
        this.post = post;
        this.user = user;
        this.parent = parent;
        this.content = content;
        this.isDeleted = false;
    }

    //  Soft Delete
    public void delete() {
        this.isDeleted = true;
    }

    public void update(String content) {
        this.content = content;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void addEmoji(CommentEmoji emoji) {
        this.emojis.add(emoji);
        emoji.setComment(this);
    }

    public void updateEmojis(List<CommentEmoji> newEmojis) {
        this.emojis.clear();
        for (CommentEmoji emoji : newEmojis) {
            addEmoji(emoji);
        }
    }
}
