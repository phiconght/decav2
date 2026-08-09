package com.trungtam.schoolclass;

import com.trungtam.exam.repository.ExamRepository;
import com.trungtam.exam.repository.ExamStudentRepository;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.report.dto.response.AttendanceSummary;
import com.trungtam.report.dto.response.ClassAttendanceReport;
import com.trungtam.report.service.ClassReportService;
import com.trungtam.report.service.StudentReportService;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.SessionStatus;
import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schedule.repository.SessionAttendanceRepository;
import com.trungtam.schoolclass.dto.response.ClassOutlineResponse;
import com.trungtam.schoolclass.dto.response.ClassOutlineResponse.OutlineTopicGroup;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.schoolclass.service.ClassOutlineService;
import com.trungtam.security.SecurityService;
import com.trungtam.subject.entity.Subject;
import com.trungtam.topic.entity.Topic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Logic gom nhom cua {@code GET /classes/&#123;id&#125;/outline}
 * (SPEC_KhoaHoc_NoiDung_Mobile §3.3).
 *
 * <p>Cac ca duoc chon vi de vo AM THAM khi sua ve sau: khoa chua gan chuyen de
 * nao (du lieu cu), thu tu nhom, va cach danh so buoi khi co buoi bi huy.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassOutlineServiceGroupingTest {

    private static final long CLASS_ID = 100L;

    @Mock private SchoolClassRepository classRepository;
    @Mock private ClassSessionRepository sessionRepository;
    @Mock private SessionAttendanceRepository attendanceRepository;
    @Mock private ExamRepository examRepository;
    @Mock private ExamStudentRepository examStudentRepository;
    @Mock private StudentParentRepository studentParentRepository;
    @Mock private SecurityService securityService;
    @Mock private StudentReportService studentReportService;
    @Mock private ClassReportService classReportService;

    @InjectMocks
    private ClassOutlineService service;

    @BeforeEach
    void setUp() {
        // Nguoi goi: admin (khong gan voi hoc vien nao -> studentId = null).
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("EXAM:READ"))));

        Subject subject = new Subject();
        subject.setName("Toán");
        subject.setGradeLevel("Khối 12");
        SchoolClass clazz = new SchoolClass();
        clazz.setId(CLASS_ID);
        clazz.setCode("LTO12-001");
        clazz.setName("Toán 12 A");
        clazz.setSubject(subject);

        when(classRepository.findById(CLASS_ID)).thenReturn(Optional.of(clazz));
        when(securityService.canViewClassContent(anyLong(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        when(examRepository.findByClassId(CLASS_ID)).thenReturn(List.of());
        // Chuyen can lay tu module Bao cao (da co logic + test rieng ben do).
        // O day chi can mot ket qua hop le; cac ca kiem tra viec GOM NHOM.
        when(classReportService.attendance(CLASS_ID, null)).thenReturn(
                new ClassAttendanceReport(
                        new AttendanceSummary(0, 0, 0, 0, 0, 0, null, null),
                        List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Topic topic(long id, String name, int sortOrder) {
        Topic t = new Topic();
        t.setId(id);
        t.setName(name);
        t.setSortOrder(sortOrder);
        return t;
    }

    private ClassSession session(long id, LocalDate date, Topic topic, SessionStatus status) {
        ClassSession s = new ClassSession();
        s.setId(id);
        s.setSessionDate(date);
        s.setStartTime(LocalTime.of(18, 0));
        s.setDurationMinutes(120);
        s.setStatus(status);
        s.setTopic(topic);
        return s;
    }

    // ============================================================

    @Test
    void emptyClass_returnsNoGroups() {
        when(sessionRepository.findByClazzId(CLASS_ID)).thenReturn(List.of());


        ClassOutlineResponse res = service.outline(CLASS_ID, null);

        assertTrue(res.groups().isEmpty(), "Khoa rong thi khong co nhom nao");
        assertEquals(0, res.progress().totalSessions());
    }

    /**
     * REGRESSION QUAN TRONG NHAT: du lieu cu (chua ai gan chuyen de) van phai
     * tra ve duoc, gom het vao 1 nhom "chua phan chuyen de" — khong duoc rong.
     */
    @Test
    void allSessionsWithoutTopic_fallIntoSingleUnassignedGroup() {
        when(sessionRepository.findByClazzId(CLASS_ID)).thenReturn(List.of(
                session(1, LocalDate.of(2026, 7, 1), null, SessionStatus.DONE),
                session(2, LocalDate.of(2026, 7, 3), null, SessionStatus.PLANNED)));


        ClassOutlineResponse res = service.outline(CLASS_ID, null);

        assertEquals(1, res.groups().size());
        OutlineTopicGroup g = res.groups().get(0);
        assertNull(g.topicId(), "Nhom chua phan chuyen de co topicId = null");
        assertNull(g.sortOrder(), "sortOrder cua nhom null phai la null");
        assertEquals(2, g.sessions().size());
    }

    @Test
    void groupsSortedBySortOrder_andUnassignedAlwaysLast() {
        Topic t1 = topic(11, "Đạo hàm", 1);
        Topic t2 = topic(22, "Tích phân", 2);
        when(sessionRepository.findByClazzId(CLASS_ID)).thenReturn(List.of(
                // Co tinh dua nhom "chua phan" va nhom sortOrder lon len TRUOC
                // trong danh sach dau vao, de bat loi neu quen sap xep.
                session(1, LocalDate.of(2026, 7, 1), null, SessionStatus.DONE),
                session(2, LocalDate.of(2026, 7, 2), t2, SessionStatus.DONE),
                session(3, LocalDate.of(2026, 7, 3), t1, SessionStatus.DONE)));


        List<OutlineTopicGroup> groups = service.outline(CLASS_ID, null).groups();

        assertEquals(3, groups.size());
        assertEquals(11L, groups.get(0).topicId(), "sortOrder 1 dung dau");
        assertEquals(22L, groups.get(1).topicId(), "sortOrder 2 dung thu hai");
        assertNull(groups.get(2).topicId(), "Nhom chua phan chuyen de LUON cuoi");
    }

    @Test
    void mixedAssignedAndUnassigned_bothPresent() {
        Topic t1 = topic(11, "Đạo hàm", 1);
        when(sessionRepository.findByClazzId(CLASS_ID)).thenReturn(List.of(
                session(1, LocalDate.of(2026, 7, 1), t1, SessionStatus.DONE),
                session(2, LocalDate.of(2026, 7, 2), null, SessionStatus.DONE)));


        List<OutlineTopicGroup> groups = service.outline(CLASS_ID, null).groups();

        assertEquals(2, groups.size());
        assertEquals(1, groups.get(0).sessions().size());
        assertEquals(1, groups.get(1).sessions().size());
    }

    /**
     * Buoi bi huy VAN duoc danh so. Neu bo buoi huy ra khoi cach danh so thi
     * moi lan huy 1 buoi, toan bo buoi phia sau bi doi so hoi to — hoc vien
     * dang nho "Buoi 3" hom sau thanh "Buoi 2".
     */
    @Test
    void cancelledSessionStillConsumesOrdinal_soNumbersStayStable() {
        Topic t1 = topic(11, "Đạo hàm", 1);
        when(sessionRepository.findByClazzId(CLASS_ID)).thenReturn(List.of(
                session(1, LocalDate.of(2026, 7, 1), t1, SessionStatus.DONE),
                session(2, LocalDate.of(2026, 7, 2), t1, SessionStatus.CANCELLED),
                session(3, LocalDate.of(2026, 7, 3), t1, SessionStatus.PLANNED)));


        List<ClassOutlineResponse.OutlineSession> sessions =
                service.outline(CLASS_ID, null).groups().get(0).sessions();

        assertEquals(1, sessions.get(0).ordinal());
        assertEquals(2, sessions.get(1).ordinal(), "Buoi bi huy van giu so cua no");
        assertEquals(3, sessions.get(2).ordinal(), "Buoi sau khong bi don so len");
    }

    @Test
    void sessionsSortedChronologically_regardlessOfInputOrder() {
        Topic t1 = topic(11, "Đạo hàm", 1);
        when(sessionRepository.findByClazzId(CLASS_ID)).thenReturn(List.of(
                session(3, LocalDate.of(2026, 7, 5), t1, SessionStatus.PLANNED),
                session(1, LocalDate.of(2026, 7, 1), t1, SessionStatus.DONE),
                session(2, LocalDate.of(2026, 7, 3), t1, SessionStatus.DONE)));


        List<ClassOutlineResponse.OutlineSession> sessions =
                service.outline(CLASS_ID, null).groups().get(0).sessions();

        assertEquals(LocalDate.of(2026, 7, 1), sessions.get(0).date());
        assertEquals(LocalDate.of(2026, 7, 3), sessions.get(1).date());
        assertEquals(LocalDate.of(2026, 7, 5), sessions.get(2).date());
    }

    /** doneSessions chi dem buoi DONE — buoi tuong lai khong duoc tinh vao tu so. */
    @Test
    void progressCountsOnlyDoneSessions() {
        Topic t1 = topic(11, "Đạo hàm", 1);
        when(sessionRepository.findByClazzId(CLASS_ID)).thenReturn(List.of(
                session(1, LocalDate.of(2026, 7, 1), t1, SessionStatus.DONE),
                session(2, LocalDate.of(2026, 7, 3), t1, SessionStatus.PLANNED),
                session(3, LocalDate.of(2026, 7, 5), t1, SessionStatus.CANCELLED)));


        ClassOutlineResponse res = service.outline(CLASS_ID, null);

        assertEquals(3, res.progress().totalSessions());
        assertEquals(1, res.progress().doneSessions());
    }
}
