package com.trungtam.announcement.service;

import com.trungtam.announcement.dto.request.CreateAnnouncementRequest;
import com.trungtam.announcement.dto.request.PreviewAudienceRequest;
import com.trungtam.announcement.dto.response.AnnouncementItem;
import com.trungtam.announcement.dto.response.AnnouncementPageResponse;
import com.trungtam.announcement.dto.response.AnnouncementResult;
import com.trungtam.announcement.entity.Announcement;
import com.trungtam.announcement.entity.AudienceType;
import com.trungtam.announcement.repository.AnnouncementRepository;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.entity.UserStatus;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.notification.entity.NotificationType;
import com.trungtam.notification.service.NotificationService;
import com.trungtam.schedule.repository.ClassRosterRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Thong bao do admin soan: resolve doi tuong nhan -> fan-out qua notify() (type ANNOUNCEMENT).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnnouncementService {

    private static final int BODY_MAX = 200;

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final ClassRosterRepository rosterRepository;
    private final StudentParentRepository studentParentRepository;
    private final NotificationService notificationService;

    /** Dem so nguoi se nhan (hien truoc khi gui). */
    public long previewCount(PreviewAudienceRequest req) {
        return resolveRecipients(req.audience(), req.audienceRef()).size();
    }

    @Transactional
    public AnnouncementResult send(CreateAnnouncementRequest req) {
        List<Long> recipients = resolveRecipients(req.audience(), req.audienceRef());

        Announcement ann = new Announcement();
        ann.setTitle(req.title());
        ann.setContentMd(req.contentMd());
        ann.setAudience(req.audience());
        ann.setAudienceRef(req.audienceRef());
        ann.setPostId(req.postId());
        Announcement saved = announcementRepository.save(ann);

        Long senderId = currentUserId();
        String body = shorten(req.contentMd());
        String payload = req.postId() != null
                ? "{\"postId\":" + req.postId() + ",\"announcementId\":" + saved.getId() + "}"
                : "{\"announcementId\":" + saved.getId() + "}";

        int sent = 0;
        for (Long recipientId : recipients) {
            boolean ok = notificationService.notifyFrom(senderId, recipientId,
                    NotificationType.ANNOUNCEMENT, req.title(), body, req.title(), req.contentMd(),
                    payload, "ANNOUNCEMENT:" + saved.getId() + ":" + recipientId).isPresent();
            if (ok) {
                sent++;
            }
        }
        saved.setSentCount(sent);
        announcementRepository.save(saved);
        return new AnnouncementResult(saved.getId(), sent);
    }

    public AnnouncementPageResponse history(int current, int pageSize) {
        int page = Math.max(0, current - 1);
        int size = pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Pageable pageable = PageRequest.of(page, size);
        return AnnouncementPageResponse.of(
                announcementRepository.findAllByOrderByIdDesc(pageable).map(AnnouncementItem::from));
    }

    // ------------------------------------------------------------------

    /** Danh sach userId nhan (distinct, giu thu tu on dinh). */
    private List<Long> resolveRecipients(AudienceType audience, String ref) {
        Set<Long> ids = new LinkedHashSet<>();
        switch (audience) {
            case ALL -> ids.addAll(userRepository.findIdsByStatus(UserStatus.ACTIVE));
            case ROLE -> ids.addAll(userRepository.findIdsByRolesAndStatus(
                    parseRoles(ref), UserStatus.ACTIVE));
            case CLASS -> {
                Long classId = parseLong(ref);
                for (Long studentId : rosterRepository.findStudentIdsByClassId(classId)) {
                    ids.add(studentId);
                    ids.addAll(studentParentRepository.findParentIdsByStudentId(studentId));
                }
            }
            case USERS -> ids.addAll(parseIds(ref));
        }
        return new ArrayList<>(ids);
    }

    private List<RoleName> parseRoles(String ref) {
        if (!StringUtils.hasText(ref)) {
            throw new AppException(ErrorCode.ANNOUNCEMENT_INVALID_AUDIENCE);
        }
        List<RoleName> roles = new ArrayList<>();
        for (String part : ref.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                roles.add(RoleName.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new AppException(ErrorCode.ANNOUNCEMENT_INVALID_AUDIENCE);
            }
        }
        if (roles.isEmpty()) {
            throw new AppException(ErrorCode.ANNOUNCEMENT_INVALID_AUDIENCE);
        }
        return roles;
    }

    private List<Long> parseIds(String ref) {
        if (!StringUtils.hasText(ref)) {
            throw new AppException(ErrorCode.ANNOUNCEMENT_INVALID_AUDIENCE);
        }
        List<Long> ids = new ArrayList<>();
        for (String part : ref.split(",")) {
            String s = part.trim();
            if (!s.isEmpty()) {
                ids.add(parseLong(s));
            }
        }
        if (ids.isEmpty()) {
            throw new AppException(ErrorCode.ANNOUNCEMENT_INVALID_AUDIENCE);
        }
        return ids;
    }

    private Long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.ANNOUNCEMENT_INVALID_AUDIENCE);
        }
    }

    /** Rut gon noi dung markdown thanh body vắn tắt cho thong bao. */
    private String shorten(String md) {
        String flat = md.replaceAll("\\s+", " ").trim();
        return flat.length() > BODY_MAX ? flat.substring(0, BODY_MAX) + "..." : flat;
    }

    private Long currentUserId() {
        String username = SecurityUtils.requireCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
    }
}
