package com.trungtam.payment.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Build chuoi payload VietQR chuan EMVCo (QR dong, mot lan dung) — SPEC_ThanhToan §2.6.
 * Khong goi dich vu ngoai: tu build TLV + CRC16-CCITT.
 *
 * <p>Cau truc TLV: moi field = ID(2) + LEN(2, 0-padded) + VALUE.
 */
@Service
public class VietQrService {

    private static final String NAPAS_AID = "A000000727";
    private static final String SERVICE_QRIBFTTA = "QRIBFTTA";
    private static final String CURRENCY_VND = "704";
    private static final String COUNTRY_VN = "VN";

    /**
     * @param bankBin       ma BIN NAPAS (vd 970436 = VCB)
     * @param accountNumber so tai khoan nhan
     * @param amount        so tien (VND nguyen)
     * @param paymentCode   noi dung CK (purpose of transaction)
     * @return chuoi payload EMVCo hoan chinh (bao gom CRC16 cuoi).
     */
    public String build(String bankBin, String accountNumber, BigDecimal amount, String paymentCode) {
        // Field 38: Merchant Account Information (NAPAS)
        String consumerAccount = tlv("00", bankBin) + tlv("01", accountNumber);      // nested 00/01
        String merchantAccount = tlv("00", NAPAS_AID)
                + tlv("01", consumerAccount)
                + tlv("02", SERVICE_QRIBFTTA);

        // Field 62: Additional data — 08 = purpose (payment_code)
        String additionalData = tlv("08", paymentCode);

        StringBuilder sb = new StringBuilder();
        sb.append(tlv("00", "01"));                    // payload format indicator
        sb.append(tlv("01", "12"));                    // point of initiation — 12 = dynamic (co so tien)
        sb.append(tlv("38", merchantAccount));         // merchant account information
        sb.append(tlv("53", CURRENCY_VND));            // transaction currency
        sb.append(tlv("54", formatAmount(amount)));    // transaction amount
        sb.append(tlv("58", COUNTRY_VN));              // country code
        sb.append(tlv("62", additionalData));          // additional data field template

        // Field 63: CRC — tinh tren toan chuoi KE CA "6304"
        sb.append("6304");
        String crc = crc16(sb.toString());
        sb.append(crc);
        return sb.toString();
    }

    /** Encode 1 field TLV: id + len(2 chu so) + value. */
    private static String tlv(String id, String value) {
        String len = String.format("%02d", value.length());
        return id + len + value;
    }

    /** So tien VND nguyen (khong lam tron le, khong dau phay). */
    private static String formatAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.DOWN).toBigInteger().toString();
    }

    /**
     * CRC16-CCITT (FALSE): init 0xFFFF, poly 0x1021, khong reflect, xorout 0.
     * Tra ve hex 4 ky tu IN HOA.
     */
    public static String crc16(String data) {
        int crc = 0xFFFF;
        byte[] bytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (byte b : bytes) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x8000) != 0) {
                    crc = (crc << 1) ^ 0x1021;
                } else {
                    crc = crc << 1;
                }
                crc &= 0xFFFF;
            }
        }
        return String.format("%04X", crc & 0xFFFF);
    }
}
