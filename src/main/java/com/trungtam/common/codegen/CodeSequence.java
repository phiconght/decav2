package com.trungtam.common.codegen;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "code_sequences")
@Getter
@Setter
@NoArgsConstructor
public class CodeSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_code", nullable = false, length = 2)
    private String subjectCode;

    @Column(name = "grade_code", nullable = false, length = 2)
    private String gradeCode;

    @Column(name = "year_offset", nullable = false)
    private int yearOffset;

    @Column(name = "last_seq", nullable = false)
    private long lastSeq = 0L;

    public static CodeSequence create(String subjectCode, String gradeCode, int yearOffset) {
        CodeSequence s = new CodeSequence();
        s.subjectCode = subjectCode;
        s.gradeCode = gradeCode;
        s.yearOffset = yearOffset;
        s.lastSeq = 0L;
        return s;
    }
}
