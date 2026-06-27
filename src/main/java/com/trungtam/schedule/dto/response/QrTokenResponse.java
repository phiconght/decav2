package com.trungtam.schedule.dto.response;

/** Token QR cua window hien tai + so giay con lai cua window. */
public record QrTokenResponse(
        String token,
        long ttlSeconds
) {
}
