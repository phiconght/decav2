package com.trungtam.identity.bootstrap;

import com.trungtam.identity.entity.Permission;
import com.trungtam.identity.entity.Role;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.repository.PermissionRepository;
import com.trungtam.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seed roles + permissions cho profile DEV (vi Flyway bi tat khi dung H2).
 * O prod, du lieu nay do Flyway seed. Chay truoc {@link AdminInitializer}.
 */
@Slf4j
@Component
@Order(1)
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleRepository.count() > 0) {
            return;
        }

        List<Permission> permissions = List.of(
                permission("USER:READ",      "USER",     "READ",   "Xem nguoi dung"),
                permission("USER:WRITE",     "USER",     "WRITE",  "Tao / sua nguoi dung"),
                permission("USER:DELETE",    "USER",     "DELETE", "Xoa nguoi dung"),
                permission("ROLE:READ",      "ROLE",     "READ",   "Xem vai tro / quyen"),
                permission("ROLE:WRITE",     "ROLE",     "WRITE",  "Gan vai tro / quyen"),
                permission("EXERCISE:READ",  "EXERCISE", "READ",   "Xem bai tap"),
                permission("EXERCISE:WRITE", "EXERCISE", "WRITE",  "Tao / sua bai tap"),
                permission("EXERCISE:DELETE","EXERCISE", "DELETE", "Xoa bai tap"),
                permission("SUBJECT:READ",   "SUBJECT",  "READ",   "Xem mon hoc"),
                permission("CLASS:READ",     "CLASS",    "READ",   "Xem lop"),
                permission("CLASS:WRITE",    "CLASS",    "WRITE",  "Tao / sua lop"),
                permission("CLASS:DELETE",   "CLASS",    "DELETE", "Xoa lop")
        );
        permissionRepository.saveAll(permissions);

        Set<Permission> all = new HashSet<>(permissions);
        for (RoleName name : RoleName.values()) {
            Role role = new Role(name, name.name());
            if (name == RoleName.ADMIN) {
                role.setPermissions(all);
            }
            roleRepository.save(role);
        }

        log.info("[DEV] Da seed {} role, {} permission (H2 in-memory)",
                RoleName.values().length, permissions.size());
    }

    private Permission permission(String code, String resource, String action, String desc) {
        Permission p = new Permission();
        p.setCode(code);
        p.setResource(resource);
        p.setAction(action);
        p.setDescription(desc);
        return p;
    }
}
