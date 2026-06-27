package com.trungtam.schedule.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Tham so loc thoi khoa bieu. view: STUDENT | TEACHER | ROOM | PARENT.
 * refId: id cua HV / GV / phong / PH theo view.
 */
@Getter
@Setter
public class TimetableQuery {

    @NotNull
    private String view;

    private Long refId;

    @NotNull
    private LocalDate from;

    @NotNull
    private LocalDate to;

    private Long branchId;
}
