package org.example.petinside.domain.post.repository;

import org.example.petinside.domain.post.entity.Category;
import org.example.petinside.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByIsDeletedFalseOrderByIdDesc();
    Page<Post> findByAuthorIdAndIsDeletedFalse(Long authorId, Pageable pageable);
    long countByCreatedAtAfter(LocalDateTime startOfDay);

    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId AND p.isDeleted = false " +
           "AND (:keyword IS NULL OR p.title LIKE %:keyword%) " +
           "AND (:category IS NULL OR p.category = :category)")
    Page<Post> searchMyPosts(@Param("authorId") Long authorId,
                              @Param("keyword") String keyword,
                              @Param("category") Category category,
                              Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false " +
           "AND (:category IS NULL OR p.category = :category) " +
           "AND (:keyword IS NULL OR p.title LIKE %:keyword%)")
    Page<Post> search(@Param("category") Category category,
                       @Param("keyword") String keyword,
                       Pageable pageable);
}
