package com.trungtam.identity.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.dto.request.ResetPasswordRequest;
import com.trungtam.identity.dto.request.UpdateUserRequest;
import com.trungtam.identity.dto.request.UserSearchParams;
import com.trungtam.identity.dto.CreateUserRequest;
import com.trungtam.identity.dto.response.UserListItem;
import com.trungtam.identity.dto.response.UserPageResponse;
import com.trungtam.identity.dto.UserResponse;
import com.trungtam.identity.entity.Role;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.entity.UserStatus;
import com.trungtam.identity.repository.RefreshTokenRepository;
import com.trungtam.identity.repository.RoleRepository;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.identity.repository.UserSpec;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private static final Set<String> SORT_WHITELIST = Set.of("username", "fullName", "createdAt", "status");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    // ---------------------------------------------------------------
    // Truy van
    // ---------------------------------------------------------------

    public UserPageResponse search(UserSearchParams params) {
        Pageable pageable = PageRequest.of(
                Math.max(params.getCurrent() - 1, 0),
                Math.min(params.getPageSize(), 100),
                resolveSort(params.getSortField(), params.getSortOrder())
        );
        Page<UserListItem> page = userRepository
                .findAll(UserSpec.build(params), pageable)
                .map(UserListItem::from);
        return UserPageResponse.of(page);
    }

    public UserResponse getById(Long id) {
        return UserResponse.from(findUser(id));
    }

    public UserResponse getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    // ---------------------------------------------------------------
    // Ghi
    // ---------------------------------------------------------------

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AppException(ErrorCode.USERNAME_EXISTS);
        }
        String email = normalizeBlank(request.email());
        if (email != null && userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(email);
        user.setPhone(normalizeBlank(request.phone()));
        user.setFullName(request.fullName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(resolveRoles(request.roles()));

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUser(id);

        // Kiem tra email trung (loai tru chinh minh)
        String email = normalizeBlank(request.email());
        if (email != null) {
            userRepository.findByEmail(email)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(__ -> { throw new AppException(ErrorCode.EMAIL_EXISTS); });
        }

        // Chi rang buoc khi vai tro THAY DOI. Sua ho ten/email/phone cua chinh minh van OK.
        Set<RoleName> currentRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        if (!currentRoles.equals(request.roles())) {
            // Khong tu doi vai tro cua chinh minh
            if (isCurrentUser(id)) {
                throw new AppException(ErrorCode.CANNOT_MODIFY_SELF);
            }
            // Khong gỡ ADMIN khoi admin cuoi cung dang ACTIVE
            boolean wasAdmin = currentRoles.contains(RoleName.ADMIN);
            boolean willBeAdmin = request.roles().contains(RoleName.ADMIN);
            if (wasAdmin && !willBeAdmin && user.getStatus() == UserStatus.ACTIVE) {
                long activeAdminCount = userRepository.countActiveAdmins(RoleName.ADMIN, UserStatus.ACTIVE);
                if (activeAdminCount <= 1) {
                    throw new AppException(ErrorCode.LAST_ADMIN);
                }
            }
        }

        user.setFullName(request.fullName());
        user.setEmail(email);
        user.setPhone(normalizeBlank(request.phone()));
        user.setRoles(resolveRoles(request.roles()));

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = findUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        // Thu hoi tat ca refresh token: buoc dang nhap lai
        refreshTokenRepository.revokeAllByUserId(id);
    }

    @Transactional
    public UserResponse updateStatus(Long userId, UserStatus status) {
        User user = findUser(userId);
        guardSelfModification(userId);

        // Khong cho vo hieu hoa admin cuoi cung (chi check khi user dang ACTIVE)
        if (status != UserStatus.ACTIVE && user.getStatus() == UserStatus.ACTIVE) {
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(r -> r.getName() == RoleName.ADMIN);
            if (isAdmin) {
                long activeAdminCount = userRepository.countActiveAdmins(RoleName.ADMIN, UserStatus.ACTIVE);
                if (activeAdminCount <= 1) {
                    throw new AppException(ErrorCode.LAST_ADMIN);
                }
            }
        }

        user.setStatus(status);
        return UserResponse.from(userRepository.save(user));
    }

    /**
     * Soft delete: chuyen trang thai sang DISABLED.
     */
    @Transactional
    public void softDelete(Long userId) {
        guardSelfModification(userId);
        User user = findUser(userId);

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ADMIN);
        if (isAdmin && user.getStatus() == UserStatus.ACTIVE) {
            long activeAdminCount = userRepository.countActiveAdmins(RoleName.ADMIN, UserStatus.ACTIVE);
            if (activeAdminCount <= 1) {
                throw new AppException(ErrorCode.LAST_ADMIN);
            }
        }

        user.setStatus(UserStatus.DISABLED);
        userRepository.save(user);
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /** Chuyen chuoi rong/null/whitespace ve null de tranh dung "" voi cot UNIQUE. */
    private static String normalizeBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        Set<Role> roles = names.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND,
                                "Vai tro khong ton tai: " + name)))
                .collect(Collectors.toCollection(HashSet::new));
        if (roles.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Phai gan it nhat mot vai tro");
        }
        return roles;
    }

    private boolean isCurrentUser(Long targetUserId) {
        String currentUsername = SecurityUtils.requireCurrentUsername();
        return userRepository.findByUsername(currentUsername)
                .map(me -> me.getId().equals(targetUserId))
                .orElse(false);
    }

    private void guardSelfModification(Long targetUserId) {
        if (isCurrentUser(targetUserId)) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_SELF);
        }
    }

    private Sort resolveSort(String sortField, String sortOrder) {
        String field = (sortField != null && SORT_WHITELIST.contains(sortField)) ? sortField : "createdAt";
        Sort.Direction dir = "ascend".equals(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }
}
