package com.trungtam.room.dto.response;

import com.trungtam.room.entity.Room;

/**
 * Payload de FE render QR dan tai phong. Prefix DECA-ROOM giup may quet
 * phan biet voi QR diem danh hoc vien (DECA-ATT).
 */
public record RoomQrResponse(
        Long roomId,
        String roomName,
        String payload
) {
    public static RoomQrResponse from(Room r) {
        return new RoomQrResponse(
                r.getId(),
                r.getName(),
                "DECA-ROOM:" + r.getId() + ":" + r.getQrCode());
    }
}
