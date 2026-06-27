package com.trungtam.schedule.service;

import com.trungtam.schedule.dto.response.QrTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Sinh / kiem tra QR token chong quet tu xa (§3.8).
 * token(sessionId, window) = base32(HMAC_SHA256(serverSecret, sessionId + ":" + window))[0..8]
 * window = floor(epochSecond / PERIOD).
 * Token doi moi PERIOD giay; chi hien tai phong -> HV khong o lop kho quet.
 */
@Service
public class QrTokenService {

    private static final String HMAC_ALGO = "HmacSHA256";
    // RFC 4648 base32 alphabet (khong padding)
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final int TOKEN_LEN = 8;

    private final String serverSecret;
    private final long periodSeconds;

    public QrTokenService(
            @Value("${app.qr.secret:dev-qr-secret-change-me}") String serverSecret,
            @Value("${app.qr.period-seconds:30}") long periodSeconds) {
        this.serverSecret = serverSecret;
        this.periodSeconds = periodSeconds <= 0 ? 30 : periodSeconds;
    }

    /** Token + ttl con lai cua window hien tai (cho GV/man lop render QR). */
    public QrTokenResponse currentToken(Long sessionId) {
        long epoch = Instant.now().getEpochSecond();
        long window = epoch / periodSeconds;
        long ttl = periodSeconds - (epoch % periodSeconds);
        return new QrTokenResponse(token(sessionId, window), ttl);
    }

    /** Hop le neu khop token window hien tai HOAC window-1 (bu lech thoi gian). */
    public boolean isValid(Long sessionId, String token) {
        if (token == null || sessionId == null) {
            return false;
        }
        long window = Instant.now().getEpochSecond() / periodSeconds;
        return constantEquals(token, token(sessionId, window))
                || constantEquals(token, token(sessionId, window - 1));
    }

    private String token(Long sessionId, long window) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(serverSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] digest = mac.doFinal((sessionId + ":" + window).getBytes(StandardCharsets.UTF_8));
            return base32(digest).substring(0, TOKEN_LEN);
        } catch (Exception e) {
            throw new IllegalStateException("Khong the sinh QR token", e);
        }
    }

    private static String base32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int idx = (buffer >> (bitsLeft - 5)) & 0x1f;
                bitsLeft -= 5;
                sb.append(BASE32[idx]);
            }
        }
        if (bitsLeft > 0) {
            int idx = (buffer << (5 - bitsLeft)) & 0x1f;
            sb.append(BASE32[idx]);
        }
        return sb.toString();
    }

    /** So sanh hang so thoi gian de tranh timing attack. */
    private static boolean constantEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
