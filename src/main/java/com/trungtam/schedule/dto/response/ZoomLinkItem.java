package com.trungtam.schedule.dto.response;

import com.trungtam.schedule.entity.SessionZoomLink;

public record ZoomLinkItem(
        Long id,
        String label,
        String zoomUrl,
        String meetingId,
        String passcode,
        int sortOrder
) {
    public static ZoomLinkItem from(SessionZoomLink link) {
        return new ZoomLinkItem(
                link.getId(),
                link.getLabel(),
                link.getZoomUrl(),
                link.getMeetingId(),
                link.getPasscode(),
                link.getSortOrder());
    }
}
