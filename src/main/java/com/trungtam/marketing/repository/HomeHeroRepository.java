package com.trungtam.marketing.repository;

import com.trungtam.marketing.entity.HomeHero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeHeroRepository extends JpaRepository<HomeHero, Long> {
}
