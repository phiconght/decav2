package com.trungtam.schedule.repository;

import com.trungtam.schoolclass.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Doc roster (class_students) phuc vu sinh buoi / diem danh / canh bao trung HV.
 * Dung native de tranh phu thuoc @ManyToMany cua SchoolClass va doc enrolled_at.
 * Khong sua SchoolClass / ClassService (file dung chung).
 */
public interface ClassRosterRepository extends JpaRepository<SchoolClass, Long> {

    @Query(value = "SELECT cs.user_id FROM class_students cs WHERE cs.class_id = :classId", nativeQuery = true)
    List<Long> findStudentIdsByClassId(@Param("classId") Long classId);

    /** Giao vien cua lop (class_teachers) — phuc vu nhac buoi cho GV. */
    @Query(value = "SELECT ct.user_id FROM class_teachers ct WHERE ct.class_id = :classId", nativeQuery = true)
    List<Long> findTeacherIdsByClassId(@Param("classId") Long classId);

    /** Cac lop HV ghi danh (phuc vu view PARENT/STUDENT khi can gop). */
    @Query(value = "SELECT cs.class_id FROM class_students cs WHERE cs.user_id = :userId", nativeQuery = true)
    List<Long> findClassIdsByStudentId(@Param("userId") Long userId);

    /** Con cua phu huynh tu student_parents (cho view PARENT, kiem quyen so huu). */
    @Query(value = "SELECT sp.student_id FROM student_parents sp WHERE sp.parent_id = :parentId", nativeQuery = true)
    List<Long> findChildrenIdsByParentId(@Param("parentId") Long parentId);
}
