package com.trungtam.video.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Query params cho GET /lecture-videos — bind qua @ModelAttribute. */
@Getter
@Setter
public class LectureVideoSearchParams {
    private Long topicId;
    private String q;
    private int current = 1;
    private int pageSize = 10;
}
