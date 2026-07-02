package com.trungtam.announcement.entity;

/**
 * Doi tuong nhan thong bao admin soan.
 * ALL: moi user ACTIVE · ROLE: theo vai tro (audienceRef = csv role) ·
 * CLASS: HV cua lop + PH cua ho (audienceRef = classId) · USERS: chon tay (audienceRef = csv userId).
 */
public enum AudienceType {
    ALL,
    ROLE,
    CLASS,
    USERS
}
