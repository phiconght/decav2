package com.trungtam.exam.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.exam.dto.PdfVariant;
import com.trungtam.exam.entity.Exam;
import com.trungtam.exam.entity.ExamExercise;
import com.trungtam.exam.entity.ExamStatus;
import com.trungtam.exam.entity.ExamStudentStatus;
import com.trungtam.exam.entity.ExamTfItemScore;
import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.exam.repository.ExamStudentRepository;
import com.trungtam.exercise.entity.ChoiceOption;
import com.trungtam.exercise.entity.Exercise;
import com.trungtam.exercise.entity.ExerciseType;
import com.trungtam.exercise.entity.TrueFalseItem;
import com.trungtam.file.service.FileService;
import com.trungtam.guardian.entity.StudentParent;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.security.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Dung file PDF de thi (xem SPEC_XuatDeThi_PDF.md).
 *
 * - 2 bien the: DE (de trang) / DAP_AN (de + dap an + thang diem).
 * - Endpoint isAuthenticated; quyen kiem trong {@link #guardAccess}:
 *   EXAM:READ -> tat ca; STUDENT/PARENT -> chi DE cua de available voi minh/con.
 * - CHI DOC: tuyet doi khong doi trang thai exam_student (tai PDF khong phai
 *   "bat dau lam bai" — khong dung ExamTakingService.getPaper).
 * - Font DejaVu Sans nhung (IDENTITY_H) de tieng Viet co dau.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExamPdfService {

    /** Be rong noi dung trang A4 sau khi tru le 56pt moi ben. */
    private static final float CONTENT_WIDTH = PageSize.A4.getWidth() - 112f;
    private static final float MAX_IMAGE_HEIGHT = 300f;
    private static final float MAX_OPTION_IMAGE_HEIGHT = 120f;
    private static final int IMAGE_MAX_BYTES = 5 * 1024 * 1024;
    private static final Duration IMAGE_TIMEOUT = Duration.ofSeconds(5);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExamRepository examRepository;
    private final ExamStudentRepository examStudentRepository;
    private final UserRepository userRepository;
    private final StudentParentRepository studentParentRepository;
    private final FileService fileService;
    private final LatexPdfRenderer latexPdfRenderer;

    private BaseFont bfRegular;
    private BaseFont bfBold;
    private BaseFont bfOblique;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @PostConstruct
    void initFonts() {
        bfRegular = loadFont("fonts/DejaVuSans.ttf");
        bfBold = loadFont("fonts/DejaVuSans-Bold.ttf");
        bfOblique = loadFont("fonts/DejaVuSans-Oblique.ttf");
    }

    private BaseFont loadFont(String resource) {
        try {
            byte[] ttf = new ClassPathResource(resource).getContentAsByteArray();
            // IDENTITY_H + EMBEDDED: bat buoc de tieng Viet co dau hien dung
            return BaseFont.createFont(resource, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    BaseFont.CACHED, ttf, null);
        } catch (Exception e) {
            throw new IllegalStateException("Khong nap duoc font PDF: " + resource, e);
        }
    }

    // ============================ ENTRY ============================

    public record ExamPdf(byte[] bytes, String asciiFilename, String utf8Filename) {}

    public ExamPdf render(Long examId, PdfVariant variant) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND));
        guardAccess(exam, variant);

        byte[] bytes = buildPdf(exam, variant);

        String prefix = variant == PdfVariant.DAP_AN ? "Dap-an" : "De-thi";
        String ascii = prefix + "_" + exam.getCode() + ".pdf";
        String fullName = (variant == PdfVariant.DAP_AN ? "Đáp án " : "Đề thi ")
                + exam.getName() + ".pdf";
        String utf8 = URLEncoder.encode(fullName, StandardCharsets.UTF_8).replace("+", "%20");
        return new ExamPdf(bytes, ascii, utf8);
    }

    // ============================ QUYEN (§4 spec) ============================

    /**
     * EXAM:READ (admin/nhan vien/GV) -> moi de, moi bien the.
     * STUDENT: chi DE, de ACTIVE + toi publishAt + co dong exam_student != DA_XOA.
     * PARENT: nhu STUDENT nhung xet theo cac con. KHONG doi trang thai gi.
     */
    private void guardAccess(Exam exam, PdfVariant variant) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean examRead = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "EXAM:READ".equals(a.getAuthority()));
        if (examRead) {
            return;
        }
        if (variant == PdfVariant.DAP_AN) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        boolean published = exam.getStatus() == ExamStatus.ACTIVE
                && exam.getPublishAt() != null
                && !exam.getPublishAt().isAfter(Instant.now());
        if (!published) {
            throw new AppException(ErrorCode.EXAM_NOT_AVAILABLE);
        }

        User me = userRepository.findByUsername(SecurityUtils.requireCurrentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        boolean isStudent = me.getRoles().stream().anyMatch(r -> r.getName() == RoleName.STUDENT);
        boolean isParent = me.getRoles().stream().anyMatch(r -> r.getName() == RoleName.PARENT);

        if (isStudent && availableFor(exam.getId(), me.getId())) {
            return;
        }
        if (isParent) {
            for (StudentParent sp : studentParentRepository.findByParentIdOrderByIdAsc(me.getId())) {
                if (availableFor(exam.getId(), sp.getStudent().getId())) {
                    return;
                }
            }
        }
        throw new AppException(ErrorCode.EXAM_NOT_AVAILABLE);
    }

    private boolean availableFor(Long examId, Long userId) {
        return examStudentRepository.findByExamIdAndUserId(examId, userId)
                .map(es -> es.getStatus() != ExamStudentStatus.DA_XOA)
                .orElse(false);
    }

    // ============================ DUNG PDF ============================

    private byte[] buildPdf(Exam exam, PdfVariant variant) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 56, 56, 56, 64);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PageNumberFooter(new Font(bfRegular, 9, Font.NORMAL, Color.GRAY)));
            doc.open();

            addHeader(doc, exam, variant);

            List<ExamExercise> sorted = exam.getExamExercises().stream()
                    .sorted(Comparator.comparingInt(ExamExercise::getSortOrder))
                    .toList();
            if (sorted.isEmpty()) {
                doc.add(spaced(new Paragraph("Đề chưa có câu hỏi.", oblique(11)), 12));
            }
            int index = 1;
            for (ExamExercise ee : sorted) {
                addQuestion(doc, writer, ee, index++, variant);
            }

            if (variant == PdfVariant.DAP_AN) {
                addAnswerTable(doc, sorted);
            }

            doc.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Loi tao PDF: " + e.getMessage());
        }
    }

    // ---- header ----

    private void addHeader(Document doc, Exam exam, PdfVariant variant) throws DocumentException {
        // Dong 1: ten trung tam (trai) — ma de (phai)
        PdfPTable top = new PdfPTable(2);
        top.setWidthPercentage(100);
        top.addCell(borderless(new Phrase("TRUNG TÂM ĐÀO TẠO", bold(11)), Element.ALIGN_LEFT));
        top.addCell(borderless(new Phrase("Mã đề: " + exam.getCode(), regular(11)), Element.ALIGN_RIGHT));
        doc.add(top);

        // Tieu de
        String title = variant == PdfVariant.DAP_AN
                ? "ĐỀ KIỂM TRA — BẢN ĐÁP ÁN (LƯU HÀNH NỘI BỘ)"
                : "ĐỀ KIỂM TRA";
        Paragraph pTitle = new Paragraph(title,
                variant == PdfVariant.DAP_AN
                        ? new Font(bfBold, 14, Font.NORMAL, new Color(0xC0, 0x26, 0x26))
                        : bold(14));
        pTitle.setAlignment(Element.ALIGN_CENTER);
        pTitle.setSpacingBefore(10);
        doc.add(pTitle);

        String subject = exam.getSubject() != null
                ? exam.getSubject().getName() + " " + exam.getSubject().getGradeLevel()
                : "";
        Paragraph pName = new Paragraph(exam.getName() + (subject.isBlank() ? "" : " — " + subject), bold(12));
        pName.setAlignment(Element.ALIGN_CENTER);
        doc.add(pName);

        if (exam.getTopic() != null) {
            Paragraph pTopic = new Paragraph("Chuyên đề: " + exam.getTopic().getName(), regular(10));
            pTopic.setAlignment(Element.ALIGN_CENTER);
            doc.add(pTopic);
        }

        StringBuilder meta = new StringBuilder();
        if (exam.getDurationMinutes() != null) {
            meta.append("Thời gian làm bài: ").append(exam.getDurationMinutes()).append(" phút");
        }
        if (exam.getPublishAt() != null) {
            if (!meta.isEmpty()) meta.append(" · ");
            LocalDate d = exam.getPublishAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
            meta.append("Ngày thi: ").append(DATE_FMT.format(d));
        }
        if (!meta.isEmpty()) {
            Paragraph pMeta = new Paragraph(meta.toString(), regular(10));
            pMeta.setAlignment(Element.ALIGN_CENTER);
            doc.add(pMeta);
        }

        if (variant == PdfVariant.DE) {
            Paragraph pStudent = new Paragraph(
                    "Họ tên học viên: ..............................................  Lớp: ....................",
                    regular(11));
            pStudent.setSpacingBefore(12);
            doc.add(pStudent);
        }

        Paragraph divider = new Paragraph(new Chunk(
                "--------------------------------------------------------------------------------",
                new Font(bfRegular, 9, Font.NORMAL, Color.GRAY)));
        divider.setAlignment(Element.ALIGN_CENTER);
        divider.setSpacingBefore(6);
        divider.setSpacingAfter(10);
        doc.add(divider);
    }

    // ---- tung cau ----

    private void addQuestion(Document doc, PdfWriter writer, ExamExercise ee, int index, PdfVariant variant)
            throws DocumentException {
        Exercise ex = ee.getExercise();
        boolean reveal = variant == PdfVariant.DAP_AN;

        String typeLabel = switch (ex.getType()) {
            case MULTIPLE_CHOICE -> "Trắc nghiệm";
            case TRUE_FALSE -> "Đúng/Sai";
            case ESSAY -> "Tự luận";
        };

        Paragraph q = new Paragraph();
        q.setSpacingBefore(10);
        q.add(new Chunk("Câu " + index + " (" + formatPoints(questionPoints(ee)) + " điểm — "
                + typeLabel + "): ", bold(11)));
        if (ex.getQuestionText() != null && !ex.getQuestionText().isBlank()) {
            latexPdfRenderer.renderMixed(writer, ex.getQuestionText(), regular(11)).forEach(q::add);
        }
        doc.add(q);

        addImage(doc, ex.getQuestionImage(), MAX_IMAGE_HEIGHT);

        switch (ex.getType()) {
            case MULTIPLE_CHOICE -> addMultipleChoice(doc, writer, ex, reveal);
            case TRUE_FALSE -> addTrueFalse(doc, writer, ee, ex, reveal);
            case ESSAY -> addEssay(doc, writer, ex, reveal);
        }
    }

    private void addMultipleChoice(Document doc, PdfWriter writer, Exercise ex, boolean reveal)
            throws DocumentException {
        List<ChoiceOption> options = ex.getOptions().stream()
                .sorted(Comparator.comparingInt(ChoiceOption::getSortOrder))
                .toList();
        char letter = 'A';
        for (ChoiceOption o : options) {
            boolean markCorrect = reveal && o.isCorrect();
            Paragraph p = new Paragraph();
            p.setIndentationLeft(16);
            p.setSpacingBefore(3);
            Font f = markCorrect ? bold(11) : regular(11);
            p.add(new Chunk(letter + ". ", f));
            if (o.getText() != null && !o.getText().isBlank()) {
                latexPdfRenderer.renderMixed(writer, o.getText(), f).forEach(p::add);
            }
            if (markCorrect) {
                p.add(new Chunk(glyphOrFallback(" ✓", " (Đúng)"), f));
            }
            doc.add(p);
            addImage(doc, o.getImage(), MAX_OPTION_IMAGE_HEIGHT);
            letter++;
        }
        if (options.isEmpty()) {
            doc.add(indented(new Paragraph("(Câu hỏi chưa có phương án)", oblique(10))));
        }
    }

    private void addTrueFalse(Document doc, PdfWriter writer, ExamExercise ee, Exercise ex, boolean reveal)
            throws DocumentException {
        List<TrueFalseItem> items = ex.getTrueFalseItems().stream()
                .sorted(Comparator.comparingInt(TrueFalseItem::getSortOrder))
                .toList();
        if (items.isEmpty()) {
            doc.add(indented(new Paragraph("(Câu hỏi chưa có ý nào)", oblique(10))));
            return;
        }
        // Anh cua tung y (neu co) in truoc bang de bang gon gang
        for (TrueFalseItem it : items) {
            addImage(doc, it.getImage(), MAX_OPTION_IMAGE_HEIGHT);
        }

        boolean withPoints = reveal && !ee.getItemScores().isEmpty();
        PdfPTable table = new PdfPTable(withPoints ? 4 : 3);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        if (withPoints) {
            table.setWidths(new float[]{70, 10, 10, 10});
        } else {
            table.setWidths(new float[]{76, 12, 12});
        }
        table.addCell(headerCell("Ý"));
        table.addCell(headerCell("Đúng"));
        table.addCell(headerCell("Sai"));
        if (withPoints) {
            table.addCell(headerCell("Điểm"));
        }

        String box = glyphOrFallback("☐", "[  ]");
        String mark = glyphOrFallback("✓", "X");
        for (TrueFalseItem it : items) {
            Phrase itemPhrase = new Phrase();
            if (it.getText() != null && !it.getText().isBlank()) {
                latexPdfRenderer.renderMixed(writer, it.getText(), regular(10)).forEach(itemPhrase::add);
            }
            table.addCell(bodyCellPhrase(itemPhrase, Element.ALIGN_LEFT));
            if (reveal) {
                table.addCell(bodyCell(it.isAnswer() ? mark : "", Element.ALIGN_CENTER, bold(10)));
                table.addCell(bodyCell(it.isAnswer() ? "" : mark, Element.ALIGN_CENTER, bold(10)));
            } else {
                table.addCell(bodyCell(box, Element.ALIGN_CENTER, regular(10)));
                table.addCell(bodyCell(box, Element.ALIGN_CENTER, regular(10)));
            }
            if (withPoints) {
                BigDecimal p = ee.getItemScores().stream()
                        .filter(s -> s.getTfItem().getId().equals(it.getId()))
                        .map(ExamTfItemScore::getPoints)
                        .findFirst()
                        .orElse(BigDecimal.ZERO);
                table.addCell(bodyCell(formatPoints(p.doubleValue()), Element.ALIGN_CENTER, regular(10)));
            }
        }
        doc.add(table);
    }

    private void addEssay(Document doc, PdfWriter writer, Exercise ex, boolean reveal) throws DocumentException {
        if (!reveal) {
            for (int i = 0; i < 8; i++) {
                Paragraph line = new Paragraph(
                        "................................................................................"
                                + "..............................",
                        new Font(bfRegular, 10, Font.NORMAL, Color.GRAY));
                line.setSpacingBefore(8);
                doc.add(line);
            }
            return;
        }
        if (ex.getEssayAnswer() != null && !ex.getEssayAnswer().isBlank()) {
            Paragraph p = new Paragraph();
            p.setIndentationLeft(16);
            p.setSpacingBefore(4);
            p.add(new Chunk("Đáp án gợi ý: ", bold(11)));
            latexPdfRenderer.renderMixed(writer, ex.getEssayAnswer(), regular(11)).forEach(p::add);
            doc.add(p);
        } else {
            doc.add(indented(new Paragraph("Đáp án gợi ý: (chấm tay)", oblique(10))));
        }
        addImage(doc, ex.getEssayAnswerImage(), MAX_IMAGE_HEIGHT);
    }

    // ---- bang dap an tong hop (DAP_AN) ----

    private void addAnswerTable(Document doc, List<ExamExercise> sorted) throws DocumentException {
        doc.newPage();
        Paragraph title = new Paragraph("BẢNG ĐÁP ÁN", bold(14));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(10);
        doc.add(title);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{12, 24, 44, 20});
        table.addCell(headerCell("Câu"));
        table.addCell(headerCell("Loại"));
        table.addCell(headerCell("Đáp án"));
        table.addCell(headerCell("Điểm"));

        double total = 0;
        int index = 1;
        for (ExamExercise ee : sorted) {
            Exercise ex = ee.getExercise();
            double points = questionPoints(ee);
            total += points;
            String answer = switch (ex.getType()) {
                case MULTIPLE_CHOICE -> mcAnswerLetter(ex);
                case TRUE_FALSE -> tfAnswerString(ex);
                case ESSAY -> "Chấm tay";
            };
            String type = switch (ex.getType()) {
                case MULTIPLE_CHOICE -> "Trắc nghiệm";
                case TRUE_FALSE -> "Đúng/Sai";
                case ESSAY -> "Tự luận";
            };
            table.addCell(bodyCell(String.valueOf(index++), Element.ALIGN_CENTER, regular(10)));
            table.addCell(bodyCell(type, Element.ALIGN_LEFT, regular(10)));
            table.addCell(bodyCell(answer, Element.ALIGN_LEFT, regular(10)));
            table.addCell(bodyCell(formatPoints(points), Element.ALIGN_CENTER, regular(10)));
        }
        doc.add(table);

        Paragraph pTotal = new Paragraph("Tổng điểm: " + formatPoints(total), bold(12));
        pTotal.setSpacingBefore(8);
        pTotal.setAlignment(Element.ALIGN_RIGHT);
        doc.add(pTotal);
    }

    private String mcAnswerLetter(Exercise ex) {
        List<ChoiceOption> options = ex.getOptions().stream()
                .sorted(Comparator.comparingInt(ChoiceOption::getSortOrder))
                .toList();
        char letter = 'A';
        for (ChoiceOption o : options) {
            if (o.isCorrect()) {
                return String.valueOf(letter);
            }
            letter++;
        }
        return "—";
    }

    private String tfAnswerString(Exercise ex) {
        List<TrueFalseItem> items = ex.getTrueFalseItems().stream()
                .sorted(Comparator.comparingInt(TrueFalseItem::getSortOrder))
                .toList();
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (TrueFalseItem it : items) {
            if (!sb.isEmpty()) sb.append(" · ");
            sb.append(i++).append(") ").append(it.isAnswer() ? "Đ" : "S");
        }
        return sb.isEmpty() ? "—" : sb.toString();
    }

    // ---- anh (§3.8 spec): loi/timeout -> placeholder, KHONG fail PDF ----

    private void addImage(Document doc, String url, float maxHeight) throws DocumentException {
        if (url == null || url.isBlank()) {
            return;
        }
        byte[] bytes = resolveImage(url);
        if (bytes == null) {
            doc.add(indented(new Paragraph("[Không tải được hình ảnh]",
                    new Font(bfOblique, 9, Font.NORMAL, Color.GRAY))));
            return;
        }
        try {
            Image img = Image.getInstance(bytes);
            img.scaleToFit(CONTENT_WIDTH, maxHeight);
            img.setSpacingBefore(4);
            img.setSpacingAfter(4);
            doc.add(img);
        } catch (Exception e) {
            doc.add(indented(new Paragraph("[Không tải được hình ảnh]",
                    new Font(bfOblique, 9, Font.NORMAL, Color.GRAY))));
        }
    }

    private byte[] resolveImage(String url) {
        try {
            int marker = url.indexOf("/api/v1/files/");
            if (marker >= 0) {
                // URL noi bo -> doc thang qua FileService, khong tu HTTP vao chinh minh
                String rest = url.substring(marker + "/api/v1/files/".length());
                long id = Long.parseLong(rest.substring(0, rest.indexOf('/')));
                try (var in = fileService.loadContent(fileService.getActive(id))) {
                    byte[] b = in.readAllBytes();
                    return b.length > IMAGE_MAX_BYTES ? null : b;
                }
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .timeout(IMAGE_TIMEOUT)
                        .GET()
                        .build();
                HttpResponse<byte[]> res = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (res.statusCode() != 200 || res.body().length > IMAGE_MAX_BYTES) {
                    return null;
                }
                return res.body();
            }
            return null;
        } catch (Exception e) {
            log.debug("Khong tai duoc anh cho PDF: {} ({})", url, e.getMessage());
            return null;
        }
    }

    // ---- diem ----

    /** Diem toi da 1 cau: TF co bang diem y -> tong diem y; nguoc lai points cua exam_exercises. */
    private double questionPoints(ExamExercise ee) {
        if (ee.getExercise().getType() == ExerciseType.TRUE_FALSE && !ee.getItemScores().isEmpty()) {
            return ee.getItemScores().stream()
                    .map(ExamTfItemScore::getPoints)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();
        }
        return ee.getPoints() != null ? ee.getPoints().doubleValue() : 0;
    }

    /** 2.0 -> "2"; 2.50 -> "2,5" (kieu Viet Nam). */
    private static String formatPoints(double value) {
        String s = value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.valueOf(value);
        return s.replace('.', ',');
    }

    // ---- font/cell helpers ----

    private Font regular(int size) {
        return new Font(bfRegular, size);
    }

    private Font bold(int size) {
        return new Font(bfBold, size);
    }

    private Font oblique(int size) {
        return new Font(bfOblique, size);
    }

    /** Chuoi co glyph dac biet: neu font khong co glyph -> fallback ASCII. */
    private String glyphOrFallback(String glyph, String fallback) {
        for (int i = 0; i < glyph.length(); i++) {
            char c = glyph.charAt(i);
            if (c != ' ' && !bfRegular.charExists(c)) {
                return fallback;
            }
        }
        return glyph;
    }

    private PdfPCell borderless(Phrase phrase, int align) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setHorizontalAlignment(align);
        return cell;
    }

    private PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, bold(10)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(0xEE, 0xEE, 0xEE));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell bodyCell(String text, int align, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(5);
        return cell;
    }

    /** Nhu {@link #bodyCell} nhung nhan san 1 Phrase (dung cho o co the chua cong thuc LaTeX). */
    private PdfPCell bodyCellPhrase(Phrase phrase, int align) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setHorizontalAlignment(align);
        cell.setPadding(5);
        return cell;
    }

    private Paragraph indented(Paragraph p) {
        p.setIndentationLeft(16);
        p.setSpacingBefore(3);
        return p;
    }

    private Paragraph spaced(Paragraph p, float before) {
        p.setSpacingBefore(before);
        return p;
    }

    /** Footer "Trang x" giua moi trang. */
    private static final class PageNumberFooter extends PdfPageEventHelper {
        private final Font font;

        private PageNumberFooter(Font font) {
            this.font = font;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            float x = (document.left() + document.right()) / 2;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase("Trang " + writer.getPageNumber(), font), x, 30, 0);
        }
    }
}
