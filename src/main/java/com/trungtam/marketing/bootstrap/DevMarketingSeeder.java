package com.trungtam.marketing.bootstrap;

import com.trungtam.marketing.entity.MarketingCategory;
import com.trungtam.marketing.entity.Testimonial;
import com.trungtam.marketing.entity.TrustStats;
import com.trungtam.marketing.repository.MarketingCategoryRepository;
import com.trungtam.marketing.repository.TestimonialRepository;
import com.trungtam.marketing.repository.TrustStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed banner marketing (danh muc + testimonial + trust bar) cho profile
 * DEV neu chua co (fallback an toan — binh thuong Flyway V41/V42 da seed
 * du lieu nay khi khoi dong). LOP HOC trong moi danh muc lay THAT tu bang
 * classes (xem MarketingService) — khong con seed rieng o day.
 */
@Slf4j
@Component
@Order(6)
@Profile("dev")
@RequiredArgsConstructor
public class DevMarketingSeeder implements ApplicationRunner {

    private final MarketingCategoryRepository categoryRepository;
    private final TestimonialRepository testimonialRepository;
    private final TrustStatsRepository trustStatsRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            categoryRepository.saveAll(java.util.List.of(
                    category("Luyện thi vào 10", "🔥", "KHỐI 9",
                            "Chinh phục kỳ thi vào 10", "#C7261C", "#FF5D6C", "#FF5D6C", "9", 0),
                    category("Khối THCS (6–9)", null, "THCS",
                            "Nền tảng vững — điểm cao", "#1E2FB8", "#2E43E8", "#2E43E8", "6,7,8,9", 1),
                    category("Khối THPT (10–12)", null, "THPT",
                            "Bứt phá điểm thi", "#0F6B45", "#2FAE7A", "#2FAE7A", "10,11,12", 2)
            ));
            log.info("[DEV] Da seed 3 danh muc marketing (fallback - Flyway chua chay?)");
        }

        if (testimonialRepository.count() == 0) {
            testimonialRepository.saveAll(java.util.List.of(
                    testimonial("Chị Hoàng Lan", "PH lớp Toán 9",
                            "Con cải thiện điểm Toán rõ rệt sau 2 tháng học tại DECA.", 0),
                    testimonial("Minh Quân", "HS lớp Lý 10",
                            "Bài kiểm tra nhanh giúp em ôn lại kiến thức ngay sau buổi học.", 1)
            ));
        }

        if (trustStatsRepository.findById(1L).isEmpty()) {
            TrustStats stats = new TrustStats();
            stats.setId(1L);
            stats.setYears("8 năm");
            stats.setStudents("1.200+");
            stats.setTeachers("35 GV");
            trustStatsRepository.save(stats);
        }
    }

    private static MarketingCategory category(String title, String emoji, String tag,
            String headline, String gStart, String gEnd, String accent, String gradeFilter, int order) {
        MarketingCategory c = new MarketingCategory();
        c.setTitle(title);
        c.setEmoji(emoji);
        c.setBannerTag(tag);
        c.setBannerHeadline(headline);
        c.setGradientStart(gStart);
        c.setGradientEnd(gEnd);
        c.setAccentColor(accent);
        c.setGradeFilter(gradeFilter);
        c.setDisplayOrder(order);
        return c;
    }

    private static Testimonial testimonial(String name, String meta, String quote, int order) {
        Testimonial t = new Testimonial();
        t.setName(name);
        t.setMeta(meta);
        t.setQuote(quote);
        t.setDisplayOrder(order);
        return t;
    }
}
