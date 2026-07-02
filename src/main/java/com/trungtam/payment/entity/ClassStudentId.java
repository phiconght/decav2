package com.trungtam.payment.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Khoa ghep (class_id, user_id) cho bang join {@code class_students} — dung boi
 * {@link ClassStudentPricing} (chi phuc vu PricingService).
 */
public class ClassStudentId implements Serializable {

    private Long clazz;
    private Long student;

    public ClassStudentId() {
    }

    public ClassStudentId(Long clazz, Long student) {
        this.clazz = clazz;
        this.student = student;
    }

    public Long getClazz() {
        return clazz;
    }

    public void setClazz(Long clazz) {
        this.clazz = clazz;
    }

    public Long getStudent() {
        return student;
    }

    public void setStudent(Long student) {
        this.student = student;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClassStudentId that)) {
            return false;
        }
        return Objects.equals(clazz, that.clazz) && Objects.equals(student, that.student);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, student);
    }
}
