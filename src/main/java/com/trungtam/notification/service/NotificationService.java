package com.trungtam.notification.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.message.entity.Message;
import com.trungtam.message.repository.MessageRepository;
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

import java.time.Instant;
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
    private final MessageRepository messageRepository;
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
        return enqueueInternal(null, recipientId, type, title, body, payload, dedupeKey, null, null);
    }

    /**
     * Phat mot su kien: tao Tin nhan (noi dung DAY DU) + Thong bao (vắn tắt) tro ve tin nhan do,
     * CUNG mot giao dich. Idempotent qua dedupeKey (bo qua ca cap neu trung).
     * Cac module goi method nay khi muon nguoi nhan doc duoc chi tiet trong hop thu.
     *
     * @return id thong bao vua tao, hoac empty neu bi bo qua (tat/im lang/trung/khong nguoi nhan).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> notify(Long recipientId, NotificationType type, String shortTitle,
                                 String shortBody, String fullTitle, String fullContent,
                                 String payload, String dedupeKey) {
        return enqueueInternal(null, recipientId, type, shortTitle, shortBody, payload, dedupeKey,
                fullTitle, fullContent);
    }

    /**
     * Nhu {@link #notify} nhung ghi nguoi gui (vd admin soan thong bao) vao Message.
     * senderId duoc gan qua proxy (getReferenceById) -> khong query them khi fan-out lon.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Long> notifyFrom(Long senderId, Long recipientId, NotificationType type,
                                     String shortTitle, String shortBody, String fullTitle,
                                     String fullContent, String payload, String dedupeKey) {
        return enqueueInternal(senderId, recipientId, type, shortTitle, shortBody, payload,
                dedupeKey, fullTitle, fullContent);
    }

    /**
     * Loi chung cho enqueue/notify. fullContent != null -> tao them Message va lien ket.
     * senderId != null -> gan nguoi gui cho Message.
     * Chay trong giao dich REQUIRES_NEW da duoc mo boi enqueue()/notify()/notifyFrom().
     */
    private Optional<Long> enqueueInternal(Long senderId, Long recipientId, NotificationType type,
                                           String title, String body, String payload,
                                           String dedupeKey, String fullTitle, String fullContent) {
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

        Message message = null;
        if (StringUtils.hasText(fullContent)) {
            message = new Message();
            message.setRecipient(recipient);
            if (senderId != null) {
                message.setSender(userRepository.getReferenceById(senderId));
            }
            message.setType(type);
            message.setTitle(StringUtils.hasText(fullTitle) ? fullTitle : title);
            message.setContent(fullContent);
            message.setPayload(payload);
            message = messageRepository.save(message);
        }

        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setPayload(payload);
        n.setDedupeKey(StringUtils.hasText(dedupeKey) ? dedupeKey : null);
        n.setStatus(NotificationStatus.PENDING);
        n.setMessage(message);
        try {
            Notification saved = notificationRepository.saveAndFlush(n);
            return Optional.of(saved.getId());
        } catch (DataIntegrityViolationException ex) {
            // Race tren unique dedupe_key -> coi nhu da co, bo qua.
            log.debug("[notify] trung dedupe_key={} -> bo qua", dedupeKey);
            return Optional.empty();
        }
    }

    /**
     * Nguoi dung da BAT tuong minh loai thong bao nay chua (co pref row va enabled=true)?
     * Dung cho loai mac dinh TAT (vd phu huynh nhan nhac buoi hoc).
     */
    public boolean isOptedIn(Long userId, NotificationType type) {
        return preferenceRepository.findByUserIdAndType(userId, type)
                .map(NotificationPreference::isEnabled)
                .orElse(false);
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

        Page<Notification> result;
        if (Boolean.TRUE.equals(params.getUnread())) {
            result = notificationRepository.findByRecipientIdAndReadAtIsNullOrderByIdDesc(userId, pageable);
        } else if (params.getStatus() != null) {
            result = notificationRepository.findByRecipientIdAndStatusOrderByIdDesc(userId, params.getStatus(), pageable);
        } else {
            result = notificationRepository.findByRecipientIdOrderByIdDesc(userId, pageable);
        }
        return NotificationPageResponse.of(result.map(NotificationItem::from));
    }

    public long unreadCount() {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(currentUserId());
    }

    @Transactional
    public void markRead(Long id) {
        Long userId = currentUserId();
        Notification n = notificationRepository.findByIdAndRecipientId(id, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead(currentUserId(), Instant.now());
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
