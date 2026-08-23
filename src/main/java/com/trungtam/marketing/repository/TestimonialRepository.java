package com.trungtam.marketing.repository;

import com.trungtam.marketing.entity.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {

    List<Testimonial> findAllByOrderByDisplayOrderAsc();
}
