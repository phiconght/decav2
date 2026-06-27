package com.trungtam.topic.entity;

import com.trungtam.subject.entity.Subject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Chuyen de — danh muc thuoc 1 mon hoc (1 mon : N chuyen de).
 * Them qua DB, khong co man CRUD; chi dung lam dropdown.
 */
@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
