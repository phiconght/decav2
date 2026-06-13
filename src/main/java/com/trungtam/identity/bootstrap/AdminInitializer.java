package com.trungtam.identity.bootstrap;

import com.trungtam.identity.entity.Role;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.entity.UserStatus;
import com.trungtam.identity.repository.RoleRepository;
import com.trungtam.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Tao tai khoan ADMIN mac dinh khi he thong chua co nguoi dung nao.
 * Mat khau duoc ma hoa bang PasswordEncoder (khong hardcode hash trong SQL).
 * <p>QUAN TRONG: doi mat khau ngay sau lan dang nhap dau tien o production.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin-password:Admin@123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Chua seed vai tro ADMIN (kiem tra Flyway V1)"));

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setFullName("System Administrator");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        log.warn("Da tao tai khoan ADMIN mac dinh: username='{}'. HAY DOI MAT KHAU NGAY.", adminUsername);
    }
}
