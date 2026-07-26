package com.trungtam.exam.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
import org.springframework.stereotype.Component;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Render cong thuc LaTeX ($...$ / $$...$$) tron trong text thuong thanh
 * chuoi {@link Chunk} de nhet vao Paragraph/Phrase cua OpenPDF — xem
 * SPEC_CongThucToan_NhapHangLoat.md §8.1.
 *
 * Cong thuc duoc ve VECTOR (khong raster) qua PdfTemplate + Graphics2D cua
 * chinh PdfWriter, dam bao net cang khi in — KHONG dung BufferedImage/raster
 * (mo khi phong to/in).
 */
@Slf4j
@Component
public class LatexPdfRenderer {

    // Cung quy uoc delimiter voi Admin ($...$/$$...$$) — xem SPEC §15.5.
    private static final Pattern MATH_PATTERN =
            Pattern.compile("\\$\\$([\\s\\S]+?)\\$\\$|\\$([^\\n$]+?)\\$");

    /**
     * Tach [text] thanh danh sach Chunk: doan thuong dung [font], doan LaTeX
     * ($...$/$$...$$) render vector bang JLaTeXMath. Loi cu phap -> Chunk
     * text tho (khong lam vo ca PDF).
     */
    public List<Chunk> renderMixed(PdfWriter writer, String text, Font font) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        Matcher m = MATH_PATTERN.matcher(text);
        int lastEnd = 0;
        while (m.find()) {
            if (m.start() > lastEnd) {
                chunks.add(new Chunk(text.substring(lastEnd, m.start()), font));
            }
            boolean isBlock = m.group(1) != null;
            String expr = (isBlock ? m.group(1) : m.group(2)).trim();
            chunks.add(renderFormulaOrFallback(writer, expr, font));
            lastEnd = m.end();
        }
        if (lastEnd < text.length()) {
            chunks.add(new Chunk(text.substring(lastEnd), font));
        }
        return chunks;
    }

    private Chunk renderFormulaOrFallback(PdfWriter writer, String expr, Font font) {
        try {
            return renderFormula(writer, expr, font);
        } catch (Exception e) {
            // Cu phap LaTeX sai hoac loi ve -> in lai nguon tho, KHONG lam
            // fail ca PDF (giong hanh vi onErrorFallback o Admin/Mobile).
            log.debug("Khong render duoc cong thuc LaTeX cho PDF: '{}' ({})", expr, e.getMessage());
            return new Chunk("$" + expr + "$", font);
        }
    }

    private Chunk renderFormula(PdfWriter writer, String expr, Font font) throws ParseException, DocumentException {
        TeXFormula formula = new TeXFormula(expr);
        // Kich thuoc cong thuc bam theo co chu dang dung, doi 1pt PDF ~ 1 diem TeX.
        float sizePt = font.getSize();
        TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_TEXT, sizePt);
        icon.setInsets(new java.awt.Insets(1, 1, 1, 1));
        int w = icon.getIconWidth();
        int h = icon.getIconHeight();
        if (w <= 0 || h <= 0) {
            return new Chunk("$" + expr + "$", font);
        }

        PdfContentByte cb = writer.getDirectContent();
        PdfTemplate template = cb.createTemplate(w, h);
        // createGraphicsShapes (khong phai createGraphics): ve glyph bang outline
        // vector that su, khong encode lai theo ma ky tu. JLaTeXMath dat ky hieu
        // toan (mui ten, quan he, chu Hy Lap...) o vi tri ma trung ASCII trong
        // font CM cua no (vd \rightarrow nam o code 33 = '!') — dung createGraphics
        // (font mode) se in nham ra ky tu ASCII do. Xem SPEC_CongThucToan_NhapHangLoat.md.
        Graphics2D g2 = template.createGraphicsShapes(w, h);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        icon.paintIcon(null, g2, 0, 0);
        g2.dispose();

        Image img = Image.getInstance(template);
        // Can chinh baseline: dich cong thuc xuong theo chieu cao de khop
        // dong text xung quanh (xap xi — OpenPDF khong co baseline API cho
        // Chunk anh, uoc luong theo 20% chieu cao).
        float offsetY = -(h * 0.2f);
        return new Chunk(img, 0, offsetY, true);
    }
}
