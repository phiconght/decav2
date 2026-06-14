package com.trungtam.exercise.entity;

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

@Entity
@Table(name = "choice_options")
@Getter
@Setter
@NoArgsConstructor
public class ChoiceOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "image", length = 500)
    private String image;

    // Ten field la "correct" de Lombok sinh isCorrect() / setCorrect() dung chuan bean convention
    @Column(name = "is_correct", nullable = false)
    private boolean correct;
}
