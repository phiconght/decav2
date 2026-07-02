package com.trungtam.post.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.post.dto.request.CreatePostRequest;
import com.trungtam.post.dto.request.PostSearchParams;
import com.trungtam.post.dto.response.PostDetail;
import com.trungtam.post.dto.response.PostItem;
import com.trungtam.post.dto.response.PostPageResponse;
import com.trungtam.post.entity.Post;
import com.trungtam.post.entity.PostStatus;
import com.trungtam.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

/**
 * Nghiep vu bai viet: feed mobile (chi PUBLISHED) + CRUD quan tri (moi trang thai).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    // ---- Mobile (cong khai, chi PUBLISHED) ----

    public PostPageResponse feed(int current, int pageSize) {
        int page = Math.max(0, current - 1);
        int size = pageSize < 1 ? 10 : Math.min(pageSize, 50);
        Sort sort = Sort.by(
                Sort.Order.desc("pinned"),
                Sort.Order.desc("publishedAt"));
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PostItem> result = postRepository
                .findByStatus(PostStatus.PUBLISHED, pageable)
                .map(PostItem::from);
        return PostPageResponse.of(result);
    }

    public PostDetail publicDetail(Long id) {
        Post post = postRepository.findByIdAndStatus(id, PostStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        return PostDetail.from(post);
    }

    // ---- Quan tri ----

    public PostPageResponse adminList(PostSearchParams params) {
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String title = params.getTitle();
        boolean hasTitle = StringUtils.hasText(title);

        Page<Post> result;
        if (params.getStatus() != null && hasTitle) {
            result = postRepository.findByStatusAndTitleContainingIgnoreCase(
                    params.getStatus(), title.trim(), pageable);
        } else if (params.getStatus() != null) {
            result = postRepository.findByStatus(params.getStatus(), pageable);
        } else if (hasTitle) {
            result = postRepository.findByTitleContainingIgnoreCase(title.trim(), pageable);
        } else {
            result = postRepository.findAll(pageable);
        }
        return PostPageResponse.of(result.map(PostItem::from));
    }

    public PostDetail adminDetail(Long id) {
        return PostDetail.from(findOrThrow(id));
    }

    @Transactional
    public PostDetail create(CreatePostRequest req) {
        Post p = new Post();
        p.setTitle(req.title());
        p.setSummary(req.summary());
        p.setCoverImageUrl(req.coverImageUrl());
        p.setContentMd(req.contentMd());
        p.setPinned(req.pinned());
        p.setStatus(PostStatus.DRAFT);
        return PostDetail.from(postRepository.save(p));
    }

    @Transactional
    public PostDetail update(Long id, CreatePostRequest req) {
        Post p = findOrThrow(id);
        p.setTitle(req.title());
        p.setSummary(req.summary());
        p.setCoverImageUrl(req.coverImageUrl());
        p.setContentMd(req.contentMd());
        p.setPinned(req.pinned());
        return PostDetail.from(postRepository.save(p));
    }

    @Transactional
    public PostDetail changeStatus(Long id, PostStatus status) {
        Post p = findOrThrow(id);
        // Dang lan dau -> ghi moc published_at (giu nguyen neu da tung dang).
        if (status == PostStatus.PUBLISHED && p.getPublishedAt() == null) {
            p.setPublishedAt(Instant.now());
        }
        p.setStatus(status);
        return PostDetail.from(postRepository.save(p));
    }

    @Transactional
    public PostDetail setPinned(Long id, boolean pinned) {
        Post p = findOrThrow(id);
        p.setPinned(pinned);
        return PostDetail.from(postRepository.save(p));
    }

    /** Xoa mem = ARCHIVED. */
    @Transactional
    public void archive(Long id) {
        Post p = findOrThrow(id);
        p.setStatus(PostStatus.ARCHIVED);
        postRepository.save(p);
    }

    private Post findOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
    }
}
