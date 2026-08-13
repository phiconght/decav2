package com.trungtam.video.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.schedule.repository.SessionVideoRepository;
import com.trungtam.topic.entity.Topic;
import com.trungtam.topic.repository.TopicRepository;
import com.trungtam.video.dto.request.LectureVideoRequest;
import com.trungtam.video.dto.request.LectureVideoSearchParams;
import com.trungtam.video.dto.response.LectureVideoItem;
import com.trungtam.video.dto.response.LectureVideoPageResponse;
import com.trungtam.video.entity.LectureVideo;
import com.trungtam.video.repository.LectureVideoRepository;
import com.trungtam.video.repository.LectureVideoSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Kho video bai giang (upload YouTube, dung lai nhieu buoi).
 * Xem SPEC_VideoBaiGiang_Zoom.md §3.1.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LectureVideoService {

    private static final Pattern YOUTUBE_URL = Pattern.compile(
            "^(https?://)?(www\\.)?(youtube\\.com/(watch\\?v=|embed/|live/)[\\w-]+|youtu\\.be/[\\w-]+).*$",
            Pattern.CASE_INSENSITIVE);

    private final LectureVideoRepository videoRepository;
    private final TopicRepository topicRepository;
    private final SessionVideoRepository sessionVideoRepository;

    public LectureVideoPageResponse search(LectureVideoSearchParams params) {
        Specification<LectureVideo> spec = LectureVideoSpec.build(params);
        int page = Math.max(0, params.getCurrent() - 1); // FE gui 1-based
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LectureVideoItem> result = videoRepository.findAll(spec, pageable).map(LectureVideoItem::from);
        return LectureVideoPageResponse.of(result);
    }

    /** Danh sach ngan gon (khong phan trang) — dung cho dropdown/search-and-add khi gan buoi. */
    public List<LectureVideoItem> quickSearch(LectureVideoSearchParams params) {
        Specification<LectureVideo> spec = LectureVideoSpec.build(params);
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "title"));
        return videoRepository.findAll(spec, pageable).map(LectureVideoItem::from).getContent();
    }

    public LectureVideoItem getById(Long id) {
        return LectureVideoItem.from(findOrThrow(id));
    }

    @Transactional
    public LectureVideoItem create(LectureVideoRequest req) {
        LectureVideo video = new LectureVideo();
        apply(video, req);
        return LectureVideoItem.from(videoRepository.save(video));
    }

    @Transactional
    public LectureVideoItem update(Long id, LectureVideoRequest req) {
        LectureVideo video = findOrThrow(id);
        apply(video, req);
        return LectureVideoItem.from(videoRepository.save(video));
    }

    @Transactional
    public void delete(Long id) {
        LectureVideo video = findOrThrow(id);
        if (sessionVideoRepository.existsByVideoId(id)) {
            throw new AppException(ErrorCode.VIDEO_IN_USE);
        }
        videoRepository.delete(video);
    }

    // ------------------------------------------------------------------

    private void apply(LectureVideo video, LectureVideoRequest req) {
        String url = req.youtubeUrl() == null ? "" : req.youtubeUrl().trim();
        if (!YOUTUBE_URL.matcher(url).matches()) {
            throw new AppException(ErrorCode.VIDEO_URL_INVALID);
        }
        boolean duplicated = video.getId() == null
                ? videoRepository.existsByYoutubeUrl(url)
                : videoRepository.existsByYoutubeUrlAndIdNot(url, video.getId());
        if (duplicated) {
            throw new AppException(ErrorCode.VIDEO_URL_DUPLICATED);
        }
        video.setTitle(req.title().trim());
        video.setYoutubeUrl(url);
        video.setDescription(req.description());
        video.setTopic(resolveTopic(req.topicId()));
    }

    private Topic resolveTopic(Long topicId) {
        if (topicId == null) {
            return null;
        }
        return topicRepository.findById(topicId)
                .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
    }

    private LectureVideo findOrThrow(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_FOUND));
    }
}
