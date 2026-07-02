package com.trungtam.message.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.message.dto.request.MessageSearchParams;
import com.trungtam.message.dto.response.MessageDetail;
import com.trungtam.message.dto.response.MessageItem;
import com.trungtam.message.dto.response.MessagePageResponse;
import com.trungtam.message.entity.Message;
import com.trungtam.message.repository.MessageRepository;
import com.trungtam.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Hop thu tin nhan (noi dung day du) cua nguoi dung dang dang nhap.
 * Ghi/tao tin nhan do NotificationService dam nhan (cung giao dich voi thong bao).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessagePageResponse listMine(MessageSearchParams params) {
        Long userId = currentUserId();
        int page = Math.max(0, params.getCurrent() - 1);
        int size = params.getPageSize() < 1 ? 10 : Math.min(params.getPageSize(), 100);
        Pageable pageable = PageRequest.of(page, size);
        boolean unread = Boolean.TRUE.equals(params.getUnread());

        Page<Message> result;
        if (params.getType() != null && unread) {
            result = messageRepository.findByRecipientIdAndTypeAndReadAtIsNullOrderByIdDesc(
                    userId, params.getType(), pageable);
        } else if (params.getType() != null) {
            result = messageRepository.findByRecipientIdAndTypeOrderByIdDesc(userId, params.getType(), pageable);
        } else if (unread) {
            result = messageRepository.findByRecipientIdAndReadAtIsNullOrderByIdDesc(userId, pageable);
        } else {
            result = messageRepository.findByRecipientIdOrderByIdDesc(userId, pageable);
        }
        return MessagePageResponse.of(result.map(MessageItem::from));
    }

    public long unreadCount() {
        return messageRepository.countByRecipientIdAndReadAtIsNull(currentUserId());
    }

    /** Xem chi tiet -> tu dong danh dau da doc. */
    @Transactional
    public MessageDetail detail(Long id) {
        Message m = requireOwned(id);
        if (m.getReadAt() == null) {
            m.setReadAt(Instant.now());
            messageRepository.save(m);
        }
        return MessageDetail.from(m);
    }

    @Transactional
    public void markRead(Long id) {
        Message m = requireOwned(id);
        if (m.getReadAt() == null) {
            m.setReadAt(Instant.now());
            messageRepository.save(m);
        }
    }

    @Transactional
    public void markAllRead() {
        messageRepository.markAllRead(currentUserId(), Instant.now());
    }

    // ---- helpers ----

    private Message requireOwned(Long id) {
        return messageRepository.findByIdAndRecipientId(id, currentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private User currentUser() {
        String username = SecurityUtils.requireCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Long currentUserId() {
        return currentUser().getId();
    }
}
