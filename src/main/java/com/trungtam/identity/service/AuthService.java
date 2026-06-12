package com.trungtam.identity.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.dto.LoginRequest;
import com.trungtam.identity.dto.RefreshRequest;
import com.trungtam.identity.dto.TokenResponse;
import com.trungtam.identity.dto.UserResponse;
import com.trungtam.identity.entity.RefreshToken;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.RefreshTokenRepository;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.security.CustomUserDetails;
import com.trungtam.security.JwtProperties;
import com.trungtam.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Nghiep vu xac thuc: dang nhap, lam moi token, dang xuat.
 * Access token la JWT stateless; refresh token la opaque token luu HASH o DB de thu hoi.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        return issueTokens(principal);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String hash = sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID));

        if (!stored.isActive()) {
            throw new AppException(ErrorCode.TOKEN_REVOKED);
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (!user.isEnabled()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        // Xoay token: thu hoi token cu, phat token moi.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(new CustomUserDetails(user));
    }

    @Transactional
    public void logout(RefreshRequest request) {
        String hash = sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private TokenResponse issueTokens(CustomUserDetails principal) {
        String accessToken = jwtService.generateAccessToken(principal);

        String rawRefresh = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken refresh = new RefreshToken();
        refresh.setUserId(principal.getId());
        refresh.setTokenHash(sha256(rawRefresh));
        refresh.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenTtlDays(), ChronoUnit.DAYS));
        refreshTokenRepository.save(refresh);

        long expiresIn = jwtProperties.accessTokenTtlMinutes() * 60;
        return TokenResponse.of(accessToken, rawRefresh, expiresIn, UserResponse.from(principal.getUser()));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            // Khong xay ra voi SHA-256; bao ve phong thu.
            return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
