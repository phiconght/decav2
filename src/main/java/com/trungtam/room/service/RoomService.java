package com.trungtam.room.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.room.dto.request.CreateRoomRequest;
import com.trungtam.room.dto.request.RoomSearchParams;
import com.trungtam.room.dto.response.RoomItem;
import com.trungtam.room.dto.response.RoomPageResponse;
import com.trungtam.room.entity.Branch;
import com.trungtam.room.entity.Room;
import com.trungtam.room.repository.BranchRepository;
import com.trungtam.room.repository.RoomRepository;
import com.trungtam.room.repository.RoomSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final BranchRepository branchRepository;

    public RoomPageResponse search(RoomSearchParams params) {
        Specification<Room> spec = RoomSpec.build(params);
        Sort sort = resolveSort(params.getSortField(), params.getSortOrder());
        int page = Math.max(0, params.getCurrent() - 1); // FE gui 1-based
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<RoomItem> result = roomRepository.findAll(spec, pageable)
                .map(RoomItem::from);
        return RoomPageResponse.of(result);
    }

    public RoomItem getById(Long id) {
        return RoomItem.from(findOrThrow(id));
    }

    /** Phong dang hoat dong cua 1 co so (dropdown). */
    public List<RoomItem> listByBranch(Long branchId) {
        return roomRepository.findByBranchIdAndActiveTrueOrderByNameAsc(branchId).stream()
                .map(RoomItem::from)
                .toList();
    }

    @Transactional
    public RoomItem create(CreateRoomRequest req) {
        Room room = new Room();
        apply(room, req);
        return RoomItem.from(roomRepository.save(room));
    }

    @Transactional
    public RoomItem update(Long id, CreateRoomRequest req) {
        Room room = findOrThrow(id);
        apply(room, req);
        return RoomItem.from(roomRepository.save(room));
    }

    // ------------------------------------------------------------------

    private Room findOrThrow(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private Branch findBranchOrThrow(Long branchId) {
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
    }

    private void apply(Room room, CreateRoomRequest req) {
        room.setCode(req.code());
        room.setName(req.name());
        room.setBranch(findBranchOrThrow(req.branchId()));
        room.setCapacity(req.capacity());
        room.setNote(req.note());
        room.setActive(req.active() == null || req.active());
    }

    private Sort resolveSort(String field, String order) {
        String sortField = switch (StringUtils.hasText(field) ? field : "name") {
            case "code" -> "code";
            case "name" -> "name";
            default -> "name";
        };
        Sort.Direction dir = "descend".equals(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, sortField);
    }
}
