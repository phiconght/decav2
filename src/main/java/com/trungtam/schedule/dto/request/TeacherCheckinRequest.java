package com.trungtam.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Ma QR phong GV quet duoc (phan code sau prefix DECA-ROOM). */
public record TeacherCheckinRequest(
        @NotBlank String roomCode
) {
}
