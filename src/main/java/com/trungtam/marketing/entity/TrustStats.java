package com.trungtam.marketing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Chi so tin cay hien o Trang chu (nam kinh nghiem / HS / GV). Bang chi co
 * 1 dong duy nhat (id = 1) — xem migration V41.
 */
@Entity
@Table(name = "trust_stats")
@Getter
@Setter
@NoArgsConstructor
public class TrustStats {

    @Id
    private Long id;

    @Column(name = "years", nullable = false, length = 20)
    private String years;

    @Column(name = "students", nullable = false, length = 20)
    private String students;

    @Column(name = "teachers", nullable = false, length = 20)
    private String teachers;
}
