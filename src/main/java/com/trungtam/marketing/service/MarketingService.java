package com.trungtam.marketing.service;

import com.trungtam.marketing.dto.response.HomeMarketingResponse;
import com.trungtam.marketing.dto.response.MarketingCategoryItem;
import com.trungtam.marketing.dto.response.TestimonialItem;
import com.trungtam.marketing.dto.response.TrustStatsResponse;
import com.trungtam.marketing.entity.MarketingCategory;
import com.trungtam.marketing.repository.MarketingCategoryRepository;
import com.trungtam.marketing.repository.TestimonialRepository;
import com.trungtam.marketing.repository.TrustStatsRepository;
import com.trungtam.schoolclass.dto.response.ClassCatalogItem;
import com.trungtam.schoolclass.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lap rap khoi marketing Trang chu mobile: banner khuyen mai (tinh), danh
 * muc theo khoi lop — LOC TU DANH SACH LOP HOC THAT (khong con du lieu
 * mock/seed rieng nhu ban dau) — trust bar, testimonial.
 *
 * <p>Danh muc va lop hoc dung CHUNG 1 nguon voi man "Khám phá khóa học"
 * ({@link ClassService#listCatalog()}) — dam bao luon dong bo.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MarketingService {

    private static final Pattern GRADE_NUM = Pattern.compile("(\\d+)");

    private final MarketingCategoryRepository categoryRepository;
    private final TestimonialRepository testimonialRepository;
    private final TrustStatsRepository trustStatsRepository;
    private final ClassService classService;

    public HomeMarketingResponse getHomeMarketing() {
        List<MarketingCategory> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        List<ClassCatalogItem> allClasses = classService.listCatalog();

        List<MarketingCategoryItem> categoryItems = categories.stream()
                .map(c -> MarketingCategoryItem.from(c, filterByGrade(allClasses, c.getGradeFilter())))
                .toList();

        List<TestimonialItem> testimonialItems = testimonialRepository
                .findAllByOrderByDisplayOrderAsc().stream()
                .map(TestimonialItem::from)
                .toList();

        TrustStatsResponse trustStats = trustStatsRepository.findById(1L)
                .map(TrustStatsResponse::from)
                .orElseGet(TrustStatsResponse::empty);

        return new HomeMarketingResponse(categoryItems, trustStats, testimonialItems);
    }

    /**
     * Loc lop theo danh sach so khoi (vd "6,7,8,9") trich tu
     * {@code Subject.gradeLevel} (dang chuoi tu do "Khối 9"). gradeFilter
     * rong/null -> khong loc (tra tat ca).
     */
    private List<ClassCatalogItem> filterByGrade(List<ClassCatalogItem> classes, String gradeFilter) {
        if (gradeFilter == null || gradeFilter.isBlank()) return classes;
        Set<String> wanted = Set.of(gradeFilter.split(","));
        return classes.stream()
                .filter(c -> {
                    String grade = extractGradeNumber(c.gradeLevel());
                    return grade != null && wanted.contains(grade);
                })
                .toList();
    }

    private String extractGradeNumber(String gradeLevel) {
        if (gradeLevel == null) return null;
        Matcher m = GRADE_NUM.matcher(gradeLevel);
        return m.find() ? m.group(1) : null;
    }
}
