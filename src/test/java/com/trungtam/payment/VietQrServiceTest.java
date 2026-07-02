package com.trungtam.payment;

import com.trungtam.payment.service.VietQrService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem chung VietQrService: EMVCo TLV + CRC16-CCITT (SPEC_ThanhToan §2.6).
 * Payload/CRC ky vong tinh doc lap va doi chieu voi thuat toan public.
 */
class VietQrServiceTest {

    private final VietQrService service = new VietQrService();

    /** CRC16-CCITT (FALSE) test vector chuan: "123456789" -> 0x29B1. */
    @Test
    void crc16_knownVector() {
        assertEquals("29B1", VietQrService.crc16("123456789"));
    }

    /** Payload mau: VCB (970436), STK 1234567890123, 1.200.000d, noi dung hv001-X7K2. */
    @Test
    void build_knownPayload() {
        String payload = service.build("970436", "1234567890123",
                new BigDecimal("1200000"), "hv001-X7K2");

        String expected =
                "00020101021238570010A00000072701270006970436011312345678901230208QRIBFTTA"
                        + "5303704540712000005802VN62140810hv001-X7K263042455";
        assertEquals(expected, payload);

        // CRC = 4 ky tu cuoi, ngay sau "6304"
        assertTrue(payload.endsWith("2455"));
        assertTrue(payload.contains("62140810hv001-X7K2"));   // field 62 -> 08 = payment_code
        assertTrue(payload.startsWith("000201"));             // field 00 = 01
        assertTrue(payload.contains("54071200000"));          // field 54 (amount) = 1200000
    }

    /** CRC luon la 4 ky tu hex in hoa; doi so tien -> doi CRC. */
    @Test
    void build_crcChangesWithAmount() {
        String a = service.build("970436", "1234567890123", new BigDecimal("1200000"), "hv001-X7K2");
        String b = service.build("970436", "1234567890123", new BigDecimal("2160000"), "hv001-X7K2");
        String crcA = a.substring(a.length() - 4);
        String crcB = b.substring(b.length() - 4);
        assertEquals(4, crcA.length());
        assertTrue(crcA.matches("[0-9A-F]{4}"));
        assertTrue(!crcA.equals(crcB));
    }
}
