package com.trungtam.notification.push;

import com.trungtam.notification.entity.DeviceToken;
import com.trungtam.notification.entity.Notification;

/**
 * Cong gui push truu tuong. Giai doan nay co the stub/log; cam SDK FCM/APNs sau
 * ma khong doi outbox / worker.
 */
public interface PushSender {

    /**
     * Gui 1 thong bao toi 1 token thiet bi.
     *
     * @return ket qua gui (thanh cong, that bai, hoac token da bi huy dang ky)
     */
    PushResult send(Notification notification, DeviceToken deviceToken);
}
