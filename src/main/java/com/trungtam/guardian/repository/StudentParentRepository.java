package com.trungtam.guardian.repository;

import com.trungtam.guardian.entity.StudentParent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentParentRepository extends JpaRepository<StudentParent, Long> {

    List<StudentParent> findByStudentIdOrderByIdAsc(Long studentId);

    List<StudentParent> findByParentIdOrderByIdAsc(Long parentId);

    Optional<StudentParent> findByStudentIdAndParentId(Long studentId, Long parentId);

    /** Cho thong bao: lay parent_id cua 1 hoc vien. */
    @Query("SELECT sp.parent.id FROM StudentParent sp WHERE sp.student.id = :studentId")
    List<Long> findParentIdsByStudentId(@Param("studentId") Long studentId);
}
