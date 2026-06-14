package com.trungtam.common.codegen;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Sinh ma dinh danh duy nhat cho bai tap va de thi.
 *
 * Dinh dang:
 *   {prefix}{monHoc}{khoiLop}-{namOffset}{4kyTu}{loai}
 *
 * Vi du:
 *   BTO11-00001N  ->  Bai tap Toan khoi 11, nam 2026, thu tu 1, trac nghiem
 *   DLY09-10002L  ->  De thi Ly khoi 9, nam 2027, thu tu 2, tu luan
 *
 * Prefix: B (bai tap), D (de thi)
 * 4 ky tu: base-36 (0-9,A-Z), tang dan, duy nhat theo (mon, khoi, nam)
 */
@Service
@RequiredArgsConstructor
public class CodeGeneratorService {

    private static final String BASE36_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SEQ_LEN = 4;
    private static final int BASE_YEAR = 2026;

    /** Mon hoc FE (ten day du) -> ma 2 ky tu */
    private static final Map<String, String> SUBJECT_CODES = Map.ofEntries(
            Map.entry("Toán",       "TO"),
            Map.entry("Vật lý",    "LY"),
            Map.entry("Hóa học",   "HO"),
            Map.entry("Tiếng Anh", "AN"),
            Map.entry("Sinh học",  "SI"),
            Map.entry("Lịch sử",   "SU"),
            Map.entry("Địa lý",    "DI"),
            Map.entry("Ngữ văn",   "VA"),
            Map.entry("Tin học",   "TI"),
            Map.entry("Thể dục",   "TD")
    );

    private final CodeSequenceRepository sequenceRepository;

    /**
     * Sinh ma bai tap (prefix = "B").
     *
     * @param subject    Ten mon hoc (theo gia tri FE, vd "Toán")
     * @param gradeLevel Ten khoi lop (vd "Khối 11")
     * @param typeChar   Ky tu loai bai: N=trac nghiem, L=tu luan, D=dung/sai
     */
    @Transactional
    public String generateExerciseCode(String subject, String gradeLevel, char typeChar) {
        return generate("B", subject, gradeLevel, typeChar);
    }

    /**
     * Sinh ma de thi (prefix = "D"). Tai su dung cung bang sequence.
     */
    @Transactional
    public String generateExamCode(String subject, String gradeLevel, char typeChar) {
        return generate("D", subject, gradeLevel, typeChar);
    }

    /**
     * Sinh ma lop thuc te (prefix = "C", typeChar = 'L').
     * Vi du: CTO10-00001L (Lop Toan Khoi 10, nam 2026, thu tu 1)
     */
    @Transactional
    public String generateClassCode(String subjectName, String gradeLevel) {
        return generate("C", subjectName, gradeLevel, 'L');
    }

    /**
     * Ham sinh ma chinh, tai su dung cho nhieu loai tai lieu.
     *
     * @param prefix     "B" hoac "D"
     * @param subject    Ten mon hoc day du
     * @param gradeLevel Ten khoi lop day du (vd "Khối 11")
     * @param typeChar   Ky tu loai cuoi ma
     */
    @Transactional
    public String generate(String prefix, String subject, String gradeLevel, char typeChar) {
        String subjectCode = toSubjectCode(subject);
        String gradeCode   = toGradeCode(gradeLevel);
        int    yearOffset  = currentYearOffset();

        long seq = nextSeq(subjectCode, gradeCode, yearOffset);
        String seqStr = toBase36(seq, SEQ_LEN);

        return prefix + subjectCode + gradeCode + "-" + yearOffset + seqStr + typeChar;
    }

    // ---- helpers ----

    private String toSubjectCode(String subject) {
        return SUBJECT_CODES.getOrDefault(subject, "XX");
    }

    /** "Khối 11" -> "11", "Khối 1" -> "01" */
    private String toGradeCode(String gradeLevel) {
        if (gradeLevel == null) return "00";
        String digits = gradeLevel.replaceAll("\\D+", "");
        if (digits.isEmpty()) return "00";
        int n = Integer.parseInt(digits);
        return String.format("%02d", n);
    }

    private int currentYearOffset() {
        int year = LocalDate.now(ZoneOffset.UTC).getYear();
        return Math.max(0, year - BASE_YEAR);
    }

    /**
     * Lay va tang sequence (PESSIMISTIC_WRITE dam bao an toan khi truy cap dong thoi).
     * Neu chua co ban ghi, tao moi voi lastSeq = 1; nguoc lai tang len 1.
     */
    private long nextSeq(String subjectCode, String gradeCode, int yearOffset) {
        CodeSequence seq = sequenceRepository
                .findForUpdate(subjectCode, gradeCode, yearOffset)
                .orElseGet(() -> CodeSequence.create(subjectCode, gradeCode, yearOffset));
        seq.setLastSeq(seq.getLastSeq() + 1);
        sequenceRepository.save(seq);
        return seq.getLastSeq();
    }

    /**
     * Chuyen so nguyen duong sang chuoi base-36 (0-9,A-Z), do dai co dinh, dem tu phai sang trai.
     * Vi du: toBase36(1, 4) = "0001", toBase36(36, 4) = "0010"
     */
    private String toBase36(long n, int length) {
        char[] result = new char[length];
        for (int i = length - 1; i >= 0; i--) {
            result[i] = BASE36_CHARS.charAt((int)(n % 36));
            n /= 36;
        }
        return new String(result);
    }
}
