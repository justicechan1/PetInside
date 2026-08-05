package org.example.petinside.cha.domain.post.repository;

import org.example.petinside.cha.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByIsDeletedFalseOrderByIdDesc();
}