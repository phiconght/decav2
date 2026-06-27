package com.trungtam.room.dto.request;

import lombok.Getter;
import lombok.Setter;

/** Query params cho GET /rooms — bind qua @ModelAttribute. */
@Getter
@Setter
public class RoomSearchParams {
    private Long branchId;
    private String keyword;
    private Boolean active;
    private int current = 1;
    private int pageSize = 10;
    private String sortField;
    private String sortOrder;
}
