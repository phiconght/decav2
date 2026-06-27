package com.trungtam.room.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.room.dto.request.CreateBranchRequest;
import com.trungtam.room.dto.response.BranchItem;
import com.trungtam.room.entity.Branch;
import com.trungtam.room.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    /** Co so dang hoat dong (dropdown). */
    public List<BranchItem> listActive() {
        return branchRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(BranchItem::from)
                .toList();
    }

    /** Tat ca co so (man quan tri). */
    public List<BranchItem> listAll() {
        return branchRepository.findAll().stream()
                .map(BranchItem::from)
                .toList();
    }

    public BranchItem getById(Long id) {
        return BranchItem.from(findOrThrow(id));
    }

    @Transactional
    public BranchItem create(CreateBranchRequest req) {
        Branch branch = new Branch();
        apply(branch, req);
        return BranchItem.from(branchRepository.save(branch));
    }

    @Transactional
    public BranchItem update(Long id, CreateBranchRequest req) {
        Branch branch = findOrThrow(id);
        apply(branch, req);
        return BranchItem.from(branchRepository.save(branch));
    }

    // ------------------------------------------------------------------

    private Branch findOrThrow(Long id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
    }

    private void apply(Branch branch, CreateBranchRequest req) {
        branch.setCode(req.code());
        branch.setName(req.name());
        branch.setAddress(req.address());
        branch.setActive(req.active() == null || req.active());
    }
}
