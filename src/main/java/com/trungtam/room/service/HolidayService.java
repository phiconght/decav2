package com.trungtam.room.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.room.dto.request.CreateHolidayRequest;
import com.trungtam.room.dto.response.HolidayItem;
import com.trungtam.room.entity.Branch;
import com.trungtam.room.entity.Holiday;
import com.trungtam.room.repository.BranchRepository;
import com.trungtam.room.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HolidayService {

    private final HolidayRepository holidayRepository;
    private final BranchRepository branchRepository;

    /** Danh sach ngay nghi trong khoang [from, to]. */
    public List<HolidayItem> list(LocalDate from, LocalDate to) {
        return holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(from, to).stream()
                .map(HolidayItem::from)
                .toList();
    }

    @Transactional
    public HolidayItem create(CreateHolidayRequest req) {
        boolean duplicated = req.branchId() == null
                ? holidayRepository.existsByHolidayDateAndBranchIsNull(req.holidayDate())
                : holidayRepository.existsByHolidayDateAndBranchId(req.holidayDate(), req.branchId());
        if (duplicated) {
            throw new AppException(ErrorCode.HOLIDAY_DUPLICATED);
        }
        Holiday holiday = new Holiday();
        holiday.setHolidayDate(req.holidayDate());
        holiday.setName(req.name());
        holiday.setBranch(resolveBranch(req.branchId()));
        return HolidayItem.from(holidayRepository.save(holiday));
    }

    @Transactional
    public void delete(Long id) {
        Holiday holiday = holidayRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        holidayRepository.delete(holiday);
    }

    /** Tap ngay nghi ap dung cho 1 co so (rieng + toan he thong) — dung boi job sinh buoi. */
    public Set<LocalDate> holidayDates(LocalDate from, LocalDate to, Long branchId) {
        List<LocalDate> dates = branchId == null
                ? holidayRepository.findGlobalDatesInRange(from, to)
                : holidayRepository.findDatesInRange(from, to, branchId);
        return Set.copyOf(dates);
    }

    // ------------------------------------------------------------------

    private Branch resolveBranch(Long branchId) {
        if (branchId == null) return null; // ngay nghi toan he thong
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
    }
}
