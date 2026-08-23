package com.trungtam.marketing.dto.response;

import com.trungtam.marketing.entity.MarketingCategory;
import com.trungtam.schoolclass.dto.response.ClassCatalogItem;

import java.util.List;

public record MarketingCategoryItem(
        Long id,
        String title,
        String emoji,
        String bannerTag,
        String bannerHeadline,
        String gradientStart,
        String gradientEnd,
        String accentColor,
        List<ClassCatalogItem> classes
) {
    public static MarketingCategoryItem from(MarketingCategory c, List<ClassCatalogItem> classes) {
        return new MarketingCategoryItem(
                c.getId(), c.getTitle(), c.getEmoji(), c.getBannerTag(),
                c.getBannerHeadline(), c.getGradientStart(), c.getGradientEnd(),
                c.getAccentColor(), classes);
    }
}
