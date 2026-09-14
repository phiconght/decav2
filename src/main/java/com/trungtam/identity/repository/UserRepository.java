package com.trungtam.identity.repository;

import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("""
        SELECT u FROM User u JOIN u.roles r
        WHERE r.name = :roleName
          AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY u.fullName ASC
        """)
    List<User> findByRoleAndKeyword(@Param("roleName") RoleName roleName,
                                    @Param("keyword") String keyword);

    @Query("""
        SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r
        WHERE r.name = :roleName AND u.status = :status
        """)
    long countActiveAdmins(@Param("roleName") RoleName roleName,
                           @Param("status") UserStatus status);

    /** Id moi user o trang thai cho truoc (audience ALL cho thong bao). */
    @Query("SELECT u.id FROM User u WHERE u.status = :status")
    List<Long> findIdsByStatus(@Param("status") UserStatus status);

    /** Id user co it nhat 1 vai tro trong danh sach, o trang thai cho truoc (audience ROLE). */
    @Query("""
        SELECT DISTINCT u.id FROM User u JOIN u.roles r
        WHERE r.name IN :roles AND u.status = :status
        """)
    List<Long> findIdsByRolesAndStatus(@Param("roles") List<RoleName> roles,
                                       @Param("status") UserStatus status);
}
