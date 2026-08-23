package com.trungtam.marketing.entity;

import com.trungtam.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 1 danh gia phu huynh/hoc sinh hien o khoi "Phụ huynh & học sinh nói gì".
 */
@Entity
@Table(name = "testimonials")
@Getter
@Setter
@NoArgsConstructor
public class Testimonial extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "meta", nullable = false, length = 150)
    private String meta;

    @Column(name = "quote", nullable = false, length = 500)
    private String quote;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
