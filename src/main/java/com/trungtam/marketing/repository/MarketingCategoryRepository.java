package com.trungtam.marketing.repository;

import com.trungtam.marketing.entity.MarketingCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MarketingCategoryRepository extends JpaRepository<MarketingCategory, Long> {

    List<MarketingCategory> findAllByOrderByDisplayOrderAsc();
}
