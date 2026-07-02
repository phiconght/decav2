package com.trungtam.payment.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.entity.User;
import com.trungtam.payment.dto.response.StudentSessionReport;
import com.trungtam.schedule.dto.response.TeacherWorkItem;
import com.trungtam.schedule.dto.response.TeacherWorkReport;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Xuat bao cao hoc phi ra .xlsx bang Apache POI (SPEC_ThanhToan §2.8).
 */
@Service
@RequiredArgsConstructor
public class PaymentExcelService {

    private static final String CENTER_NAME = "TRUNG TAM DAO TAO";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final PaymentReportService reportService;

    /** Sheet HV: chuyen can + don gia buoi. */
    public byte[] exportStudentSessions(Long studentId, LocalDate from, LocalDate to) {
        User student = reportService.findStudentOrThrow(studentId);
        StudentSessionReport report = reportService.studentSessions(studentId, from, to);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Hoc vien");
            CellStyle title = titleStyle(wb);
            CellStyle header = headerStyle(wb);
            CellStyle bold = boldStyle(wb);

            int r = 0;
            r = writeTitleBlock(sheet, title, bold, r, "BAO CAO CHUYEN CAN",
                    student, from, to);
            r++;

            // Header bang
            String[] cols = {"STT", "Ngay", "Lop", "Gio", "Trang thai", "Don gia buoi"};
            Row hr = sheet.createRow(r++);
            for (int c = 0; c < cols.length; c++) {
                Cell cell = hr.createCell(c);
                cell.setCellValue(cols[c]);
                cell.setCellStyle(header);
            }

            int stt = 1;
            for (StudentSessionReport.Item it : report.items()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(it.date() != null ? it.date().format(DATE_FMT) : "");
                row.createCell(2).setCellValue(it.className() != null ? it.className() : "");
                String gio = (it.startTime() != null ? it.startTime().format(TIME_FMT) : "")
                        + (it.endTime() != null ? "-" + it.endTime().format(TIME_FMT) : "");
                row.createCell(3).setCellValue(gio);
                row.createCell(4).setCellValue(it.status());
                row.createCell(5).setCellValue(toDouble(it.price()));
            }

            // Dong tong
            StudentSessionReport.Summary s = report.summary();
            r++;
            writeKeyValue(sheet, bold, r++, "Tong buoi (DONE)", String.valueOf(s.total()));
            writeKeyValue(sheet, bold, r++, "Co mat", String.valueOf(s.coMat()));
            writeKeyValue(sheet, bold, r++, "Di tre", String.valueOf(s.tre()));
            writeKeyValue(sheet, bold, r++, "Vang", String.valueOf(s.vang()));
            writeKeyValue(sheet, bold, r++, "Co phep", String.valueOf(s.coPhep()));
            writeKeyValue(sheet, bold, r++, "Chua diem danh", String.valueOf(s.chuaCheckin()));
            writeKeyValue(sheet, bold, r++, "Tong tien cac buoi tinh phi",
                    formatVnd(s.totalAmount()) + " d");

            autosize(sheet, cols.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Loi xuat Excel");
        }
    }

    /** Sheet GV: cong day. */
    public byte[] exportTeacherSessions(Long teacherId, LocalDate from, LocalDate to) {
        User teacher = reportService.findUserOrThrow(teacherId);
        TeacherWorkReport report = reportService.teacherSessions(teacherId, from, to);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Giao vien");
            CellStyle title = titleStyle(wb);
            CellStyle header = headerStyle(wb);
            CellStyle bold = boldStyle(wb);

            int r = 0;
            r = writeTitleBlock(sheet, title, bold, r, "BAO CAO CONG DAY",
                    teacher, from, to);
            r++;

            String[] cols = {"STT", "Ngay", "Lop", "Phong", "Gio", "Thoi luong (phut)",
                    "Trang thai cong", "Gio vao", "Gio ra"};
            Row hr = sheet.createRow(r++);
            for (int c = 0; c < cols.length; c++) {
                Cell cell = hr.createCell(c);
                cell.setCellValue(cols[c]);
                cell.setCellStyle(header);
            }

            int stt = 1;
            for (TeacherWorkItem it : report.items()) {
                Row row = sheet.createRow(r++);
                row.createCell(0).setCellValue(stt++);
                row.createCell(1).setCellValue(it.date() != null ? it.date().format(DATE_FMT) : "");
                row.createCell(2).setCellValue(it.className() != null ? it.className() : "");
                row.createCell(3).setCellValue(it.roomName() != null ? it.roomName() : "");
                String gio = (it.startTime() != null ? it.startTime().format(TIME_FMT) : "")
                        + (it.endTime() != null ? "-" + it.endTime().format(TIME_FMT) : "");
                row.createCell(4).setCellValue(gio);
                row.createCell(5).setCellValue(it.durationMinutes() != null ? it.durationMinutes() : 0);
                row.createCell(6).setCellValue(it.status());
                row.createCell(7).setCellValue(it.checkInAt() != null ? it.checkInAt().toString() : "");
                row.createCell(8).setCellValue(it.checkOutAt() != null ? it.checkOutAt().toString() : "");
            }

            TeacherWorkReport.Summary s = report.summary();
            r++;
            writeKeyValue(sheet, bold, r++, "Tong buoi", String.valueOf(s.totalSessions()));
            writeKeyValue(sheet, bold, r++, "Dung gio", String.valueOf(s.dungGio()));
            writeKeyValue(sheet, bold, r++, "Vao tre", String.valueOf(s.vaoTre()));
            writeKeyValue(sheet, bold, r++, "Vang", String.valueOf(s.vang()));
            writeKeyValue(sheet, bold, r++, "Chua cham", String.valueOf(s.chuaCham()));
            writeKeyValue(sheet, bold, r++, "Tong phut day", String.valueOf(s.totalTaughtMinutes()));

            autosize(sheet, cols.length);
            return toBytes(wb);
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Loi xuat Excel");
        }
    }

    // ---- helpers ----

    private int writeTitleBlock(Sheet sheet, CellStyle title, CellStyle bold, int r,
                                String reportName, User person, LocalDate from, LocalDate to) {
        Row r0 = sheet.createRow(r++);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(CENTER_NAME);
        c0.setCellStyle(title);
        sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 5));

        Row r1 = sheet.createRow(r++);
        Cell c1 = r1.createCell(0);
        c1.setCellValue(reportName);
        c1.setCellStyle(title);
        sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 5));

        writeKeyValue(sheet, bold, r++, "Ho ten",
                (person.getFullName() != null ? person.getFullName() : "") + " (" + person.getUsername() + ")");
        writeKeyValue(sheet, bold, r++, "Ky bao cao",
                (from != null ? from.format(DATE_FMT) : "") + " - " + (to != null ? to.format(DATE_FMT) : ""));
        return r;
    }

    private void writeKeyValue(Sheet sheet, CellStyle bold, int r, String key, String value) {
        Row row = sheet.createRow(r);
        Cell k = row.createCell(0);
        k.setCellValue(key);
        k.setCellStyle(bold);
        row.createCell(1).setCellValue(value);
    }

    private CellStyle titleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle boldStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private void autosize(Sheet sheet, int cols) {
        for (int c = 0; c < cols; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    private byte[] toBytes(Workbook wb) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private static double toDouble(BigDecimal v) {
        return v != null ? v.doubleValue() : 0d;
    }

    private static String formatVnd(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return String.format("%,d", amount.toBigInteger()).replace(',', '.');
    }
}
