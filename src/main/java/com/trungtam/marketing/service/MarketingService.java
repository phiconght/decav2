package com.trungtam.marketing.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.marketing.dto.request.HomeHeroUpdateRequest;
import com.trungtam.marketing.dto.response.HomeHeroResponse;
import com.trungtam.marketing.dto.response.HomeMarketingResponse;
import com.trungtam.marketing.dto.response.MarketingCategoryItem;
import com.trungtam.marketing.dto.response.TestimonialItem;
import com.trungtam.marketing.dto.response.TrustStatsResponse;
import com.trungtam.marketing.entity.HomeHero;
import com.trungtam.marketing.entity.MarketingCategory;
import com.trungtam.marketing.repository.HomeHeroRepository;
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
 * Lap rap khoi marketing Trang chu: hero (Trang chu cong khai Web), banner
 * khuyen mai (tinh), danh muc theo khoi lop — LOC TU DANH SACH LOP HOC THAT
 * (khong con du lieu mock/seed rieng nhu ban dau) — trust bar, testimonial.
 *
 * <p>Danh muc va lop hoc dung CHUNG 1 nguon voi man "Khám phá khóa học"
 * ({@link ClassService#listCatalog()}) — dam bao luon dong bo.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MarketingService {

    private static final Long HERO_ID = 1L;
    private static final Pattern GRADE_NUM = Pattern.compile("(\\d+)");

    private final MarketingCategoryRepository categoryRepository;
    private final TestimonialRepository testimonialRepository;
    private final TrustStatsRepository trustStatsRepository;
    private final HomeHeroRepository homeHeroRepository;
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

        HomeHeroResponse hero = homeHeroRepository.findById(HERO_ID)
                .filter(HomeHero::isVisible)
                .map(HomeHeroResponse::from)
                .orElse(null);

        return new HomeMarketingResponse(hero, categoryItems, trustStats, testimonialItems);
    }

    /** Doc noi dung Hero cho man quan tri ADMIN — luon tra ke ca khi visible = false. */
    public HomeHeroResponse getHeroForAdmin() {
        return homeHeroRepository.findById(HERO_ID)
                .map(HomeHeroResponse::from)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    /** Sua noi dung Hero — ADMIN/EMPLOYEE (quyen MARKETING:WRITE). */
    @Transactional
    public HomeHeroResponse updateHero(HomeHeroUpdateRequest req) {
        HomeHero hero = homeHeroRepository.findById(HERO_ID).orElseGet(() -> {
            HomeHero h = new HomeHero();
            h.setId(HERO_ID);
            return h;
        });
        hero.setBadgeText(req.badgeText());
        hero.setTitle(req.title());
        hero.setSubtitle(req.subtitle());
        hero.setPrimaryCtaLabel(req.primaryCtaLabel());
        hero.setPrimaryCtaHref(req.primaryCtaHref());
        hero.setSecondaryCtaLabel(req.secondaryCtaLabel());
        hero.setSecondaryCtaHref(req.secondaryCtaHref());
        hero.setBackgroundImageUrl(req.backgroundImageUrl());
        hero.setVisible(req.visible());
        return HomeHeroResponse.from(homeHeroRepository.save(hero));
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
