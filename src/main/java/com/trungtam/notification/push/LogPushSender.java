package com.trungtam.notification.push;

import com.trungtam.notification.entity.DeviceToken;
import com.trungtam.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Impl stub: chi log thong bao thay vi goi FCM/APNs that.
 * Cam SDK that bang cach cung cap mot {@link PushSender} khac (vd @Primary) sau nay.
 */
@Slf4j
@Service
public class LogPushSender implements PushSender {

    @Override
    public PushResult send(Notification notification, DeviceToken deviceToken) {
        log.info("[push-stub] gui notif id={} type={} -> token={} platform={} title='{}' body='{}'",
                notification.getId(),
                notification.getType(),
                deviceToken.getToken(),
                deviceToken.getPlatform(),
                notification.getTitle(),
                notification.getBody());
        return PushResult.success();
    }
}
