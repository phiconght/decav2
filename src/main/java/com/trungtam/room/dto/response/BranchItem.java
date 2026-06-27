package com.trungtam.room.dto.response;

import com.trungtam.room.entity.Branch;

public record BranchItem(
        Long id,
        String code,
        String name,
        String address,
        boolean active
) {
    public static BranchItem from(Branch b) {
        return new BranchItem(b.getId(), b.getCode(), b.getName(), b.getAddress(), b.isActive());
    }
}
