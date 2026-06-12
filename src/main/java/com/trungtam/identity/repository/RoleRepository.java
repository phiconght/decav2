package com.trungtam.identity.repository;

import com.trungtam.identity.entity.Role;
import com.trungtam.identity.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
