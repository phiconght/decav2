package com.trungtam.schoolclass.repository;

import com.trungtam.identity.entity.User;
import com.trungtam.schoolclass.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long>, JpaSpecificationExecutor<SchoolClass> {

    boolean existsBySubjectId(Long subjectId);

    /** Ghi ngay ghi danh cho cac cap (class_id,user_id) moi (cot phu tren bang @ManyToMany). */
    @Modifying
    @Query(value = "UPDATE class_students SET enrolled_at = CURRENT_DATE "
            + "WHERE class_id = :classId AND user_id IN (:userIds) AND enrolled_at IS NULL",
            nativeQuery = true)
    void markEnrolledAt(@Param("classId") Long classId, @Param("userIds") List<Long> userIds);

    @Query("SELECT COUNT(u) FROM SchoolClass c JOIN c.students u WHERE c.id = :classId")
    long countStudents(@Param("classId") Long classId);

    @Query("SELECT DISTINCT u FROM SchoolClass c JOIN c.students u WHERE c.id IN :classIds ORDER BY u.fullName ASC")
    List<User> findStudentsByClassIds(@Param("classIds") List<Long> classIds);

    @Query("SELECT c FROM SchoolClass c JOIN c.students u WHERE u.id = :userId ORDER BY c.createdAt DESC")
    List<SchoolClass> findClassesByStudentId(@Param("userId") Long userId);

    @Query("SELECT c FROM SchoolClass c JOIN c.teachers t WHERE t.id = :userId ORDER BY c.createdAt DESC")
    List<SchoolClass> findClassesByTeacherId(@Param("userId") Long userId);
}
