package com.trungtam.post.repository;

import com.trungtam.post.entity.Post;
import com.trungtam.post.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /** Feed mobile: chi bai PUBLISHED (sort do Pageable: pinned desc, published_at desc). */
    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    Page<Post> findByStatusAndTitleContainingIgnoreCase(
            PostStatus status, String title, Pageable pageable);

    Page<Post> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Optional<Post> findByIdAndStatus(Long id, PostStatus status);
}
