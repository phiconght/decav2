package com.trungtam.video.repository;

import com.trungtam.video.entity.LectureVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LectureVideoRepository
        extends JpaRepository<LectureVideo, Long>, JpaSpecificationExecutor<LectureVideo> {

    boolean existsByYoutubeUrl(String youtubeUrl);

    boolean existsByYoutubeUrlAndIdNot(String youtubeUrl, Long id);
}
