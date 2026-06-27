package com.trungtam.room.dto.response;

import com.trungtam.room.entity.Room;

public record RoomItem(
        Long id,
        String code,
        String name,
        Long branchId,
        String branchName,
        Integer capacity,
        String note,
        boolean active
) {
    public static RoomItem from(Room r) {
        return new RoomItem(
                r.getId(),
                r.getCode(),
                r.getName(),
                r.getBranch().getId(),
                r.getBranch().getName(),
                r.getCapacity(),
                r.getNote(),
                r.isActive());
    }
}
