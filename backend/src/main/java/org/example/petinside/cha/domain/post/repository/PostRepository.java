package org.example.petinside.cha.domain.post.repository;

import org.example.petinside.cha.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    // 게시글 목록 최신순 조회 (Soft Delete 고려)
    List<Post> findAllByIsDeletedFalseOrderByIdDesc();
}