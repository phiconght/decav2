package com.trungtam.room.dto.response;

import com.trungtam.room.entity.Holiday;

import java.time.LocalDate;

public record HolidayItem(
        Long id,
        LocalDate holidayDate,
        String name,
        Long branchId,
        String branchName
) {
    public static HolidayItem from(Holiday h) {
        return new HolidayItem(
                h.getId(),
                h.getHolidayDate(),
                h.getName(),
                h.getBranch() != null ? h.getBranch().getId() : null,
                h.getBranch() != null ? h.getBranch().getName() : null);
    }
}
