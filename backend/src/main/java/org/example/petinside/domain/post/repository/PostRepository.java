package org.example.petinside.domain.post.repository;

import org.example.petinside.domain.post.entity.Category;
import org.example.petinside.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false "
            + "AND (:category IS NULL OR p.category = :category) "
            + "AND (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%') OR p.content LIKE CONCAT('%', :keyword, '%'))")
    Page<Post> search(@Param("category") Category category, @Param("keyword") String keyword, Pageable pageable);
}