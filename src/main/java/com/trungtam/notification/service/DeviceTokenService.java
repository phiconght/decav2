package com.trungtam.notification.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.notification.dto.request.RegisterDeviceRequest;
import com.trungtam.notification.dto.request.UnregisterDeviceRequest;
import com.trungtam.notification.entity.DeviceToken;
import com.trungtam.notification.repository.DeviceTokenRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Quan ly token thiet bi cua nguoi dung dang dang nhap (dang ky / huy dang ky).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    /**
     * Dang ky token cho user hien tai. Idempotent theo token (unique):
     * token da ton tai -> gan lai chu so huu / platform va bat active.
     */
    @Transactional
    public void register(RegisterDeviceRequest req) {
        User user = currentUser();
        DeviceToken device = deviceTokenRepository.findByToken(req.token())
                .orElseGet(DeviceToken::new);
        device.setUser(user);
        device.setToken(req.token());
        device.setPlatform(req.platform());
        device.setActive(true);
        deviceTokenRepository.save(device);
    }

    /** Huy dang ky token (vd dang xuat). Idempotent: khong co thi bo qua. */
    @Transactional
    public void unregister(UnregisterDeviceRequest req) {
        Optional<DeviceToken> deviceOpt = deviceTokenRepository.findByToken(req.token());
        deviceOpt.ifPresent(device -> {
            device.setActive(false);
            deviceTokenRepository.save(device);
        });
    }

    private User currentUser() {
        String username = SecurityUtils.requireCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
