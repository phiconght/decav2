package com.trungtam.notification.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.notification.dto.request.NotificationSearchParams;
import com.trungtam.notification.dto.request.UpdatePreferenceRequest;
import com.trungtam.notification.dto.request.UpdatePreferencesRequest;
import com.trungtam.notification.dto.response.NotificationItem;
import com.trungtam.notification.dto.response.NotificationPageResponse;
import com.trungtam.notification.dto.response.PreferenceItem;
import com.trungtam.notification.entity.Notification;
import com.trungtam.notification.entity.NotificationPreference;
import com.trungtam.notification.entity.NotificationStatus;
import com.trungtam.notification.entity.NotificationType;
import com.trungtam.notification.repository.NotificationPreferenceRepository;
import com.trungtam.notification.repository.NotificationRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Diem vao outbox thong bao: enqueue (cac module khac goi), doc lich su va tuy chon
 * cua nguoi dung hien tai.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    /**
     * Dua mot thong bao vao outbox (PENDING). Cac module khac (schedule, leave...) goi method nay.
     * <ul>
     *   <li>Ton trong tuy chon: tat (enabled=false) -> bo qua; trong gio im lang -> bo qua.</li>
     *   <li>dedupeKey != null va da ton tai -> bo qua (idempotent), bat ca unique-violation khi race.</li>
     * </ul>
     * Chay trong giao dich rieng (REQUIRES_NEW) de khong keo theo rollback cua giao dich goi
     * khi gap trung dedupe_key.
     *
     * @return id ban ghi vua tao, hoac empty neu bi bo qua (tat/im lang/trung).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> enqueue(Long recipientId, NotificationType type, String title,
                                  String body, String payload, String dedupeKey) {
        if (recipientId == null || type == null) {
            return Optional.empty();
        }
        if (StringUtils.hasText(dedupeKey) && notificationRepository.existsByDedupeKey(dedupeKey)) {
            return Optional.empty();
        }
        if (!isAllowedNow(recipientId, type)) {
            log.debug("[notify] bo qua enqueue type={} recipient={} (tat hoac gio im lang)", type, recipientId);
            return Optional.empty();
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setPayload(payload);
        n.setDedupeKey(StringUtils.hasText(dedupeKey) ? dedupeKey : null);
        n.setStatus(NotificationStatus.PENDING);
        try {
            Notification saved = notificationRepository.saveAndFlush(n);
            return Optional.of(saved.getId());
        } catch (DataIntegrityViolationException ex) {
            // Race tren unique dedupe_key -> coi nhu da co, bo qua.
            log.debug("[notify] trung dedupe_key={} -> bo qua", dedupeKey);
            return Optional.empty();
        }
    }

    /** Kiem tra tuy chon: bat hay khong, va co dang trong khung gio im lang khong. */
    private boolean isAllowedNow(Long userId, NotificationType type) {
        Optional<NotificationPreference> prefOpt = preferenceRepository.findByUserIdAndType(userId, type);
        if (prefOpt.isEmpty()) {
            return true; // mac dinh nhan tat ca neu chua cau hinh
        }
        NotificationPreference pref = prefOpt.get();
        if (!pref.isEnabled()) {
            return false;
        }
        return !isInQuietHours(pref.getQuietFrom(), pref.getQuietTo(), LocalTime.now());
    }

    /**
     * Trong khung gio im lang? Ho tro khung vat qua nua dem (vd 22:00 -> 07:00).
     * Thieu mot trong hai moc -> coi nhu khong cau hinh im lang.
     */
    static boolean isInQuietHours(LocalTime from, LocalTime to, LocalTime now) {
        if (from == null || to == null || from.equals(to)) {
            return false;
        }
        if (from.isBefore(to)) {
            return !now.isBefore(from) && now.isBefore(to);
        }
        // vat qua nua dem
        return !now.isBefore(from) || now.isBefore(to);
    }

    // ---- API cho nguoi dung hien tai ----

    public NotificationPageResponse listMine(NotificationSearchParams params) {
        Long userId = currentUserId();
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Notification> result = (params.getStatus() == null)
                ? notificationRepository.findByRecipientIdOrderByIdDesc(userId, pageable)
                : notificationRepository.findByRecipientIdAndStatusOrderByIdDesc(userId, params.getStatus(), pageable);
        return NotificationPageResponse.of(result.map(NotificationItem::from));
    }

    public List<PreferenceItem> listMyPreferences() {
        Long userId = currentUserId();
        return preferenceRepository.findByUserIdOrderByTypeAsc(userId).stream()
                .map(PreferenceItem::from)
                .toList();
    }

    @Transactional
    public List<PreferenceItem> updateMyPreferences(UpdatePreferencesRequest req) {
        User user = currentUser();
        for (UpdatePreferenceRequest item : req.items()) {
            NotificationPreference pref = preferenceRepository
                    .findByUserIdAndType(user.getId(), item.type())
                    .orElseGet(() -> {
                        NotificationPreference p = new NotificationPreference();
                        p.setUser(user);
                        p.setType(item.type());
                        return p;
                    });
            pref.setEnabled(Boolean.TRUE.equals(item.enabled()));
            pref.setQuietFrom(item.quietFrom());
            pref.setQuietTo(item.quietTo());
            preferenceRepository.save(pref);
        }
        return listMyPreferences();
    }

    // ---- helpers ----

    private User currentUser() {
        String username = SecurityUtils.requireCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Long currentUserId() {
        return currentUser().getId();
    }
}
