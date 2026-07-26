package com.trungtam.security;

import com.trungtam.guardian.entity.StudentParent;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.identity.entity.User;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Guard cho NOI DUNG khoa hoc — dung chung boi
 * {@code GET /classes/&#123;id&#125;/outline} va {@code GET /exams/by-class/&#123;id&#125;}.
 *
 * <p>Day la bai test bao ve lo hong da vá o SPEC_KhoaHoc_NoiDung_Mobile §3.5:
 * truoc khi vá, bat ky user da dang nhap nao cung liet ke duoc de thi cua BAT
 * KY lop nao. Neu ai do lam hong ham nay, cac ca "lop cua nguoi khac" duoi day
 * se do.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityServiceCanViewClassContentTest {

    private static final long MY_ID = 10L;
    private static final long MY_CLASS = 100L;
    private static final long OTHER_CLASS = 999L;
    private static final long MY_CHILD = 20L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentParentRepository studentParentRepository;
    @Mock
    private SchoolClassRepository schoolClassRepository;

    @InjectMocks
    private SecurityService service;

    @BeforeEach
    void setUp() {
        User me = new User();
        me.setId(MY_ID);
        when(userRepository.findByUsername("me")).thenReturn(Optional.of(me));
        // Mac dinh: khong day lop nao, khong hoc lop nao -> tung ca tu bat len.
        lenient().when(schoolClassRepository.existsByIdAndTeachers_Id(anyLong(), anyLong()))
                .thenReturn(false);
        lenient().when(schoolClassRepository.existsByIdAndStudents_Id(anyLong(), anyLong()))
                .thenReturn(false);
        lenient().when(studentParentRepository.findByParentIdOrderByIdAsc(anyLong()))
                .thenReturn(List.of());
    }

    private Authentication auth(String... roles) {
        return new UsernamePasswordAuthenticationToken("me", null,
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
    }

    // ---------------- staff: khong gioi han ----------------

    @Test
    void admin_seesAnyClass() {
        assertTrue(service.canViewClassContent(OTHER_CLASS, auth("ROLE_ADMIN")));
    }

    @Test
    void employee_seesAnyClass() {
        assertTrue(service.canViewClassContent(OTHER_CLASS, auth("ROLE_EMPLOYEE")));
    }

    // ---------------- giao vien: chi lop minh day ----------------

    @Test
    void teacher_seesOwnClass() {
        when(schoolClassRepository.existsByIdAndTeachers_Id(MY_CLASS, MY_ID)).thenReturn(true);
        assertTrue(service.canViewClassContent(MY_CLASS, auth("ROLE_TEACHER")));
    }

    @Test
    void teacher_blockedOnOtherClass() {
        assertFalse(service.canViewClassContent(OTHER_CLASS, auth("ROLE_TEACHER")));
    }

    @Test
    void assistant_seesOwnClass() {
        when(schoolClassRepository.existsByIdAndTeachers_Id(MY_CLASS, MY_ID)).thenReturn(true);
        assertTrue(service.canViewClassContent(MY_CLASS, auth("ROLE_ASSISTANT")));
    }

    // ---------------- hoc vien: chi lop minh hoc ----------------

    @Test
    void student_seesOwnClass() {
        when(schoolClassRepository.existsByIdAndStudents_Id(MY_CLASS, MY_ID)).thenReturn(true);
        assertTrue(service.canViewClassContent(MY_CLASS, auth("ROLE_STUDENT")));
    }

    /** Ca quan trong nhat: truoc khi vá, day tra ve du lieu thay vi bi chan. */
    @Test
    void student_blockedOnClassTheyDoNotAttend() {
        assertFalse(service.canViewClassContent(OTHER_CLASS, auth("ROLE_STUDENT")));
    }

    // ---------------- phu huynh: theo lop cua con ----------------

    @Test
    void parent_seesClassOfTheirChild() {
        StudentParent link = new StudentParent();
        User child = new User();
        child.setId(MY_CHILD);
        link.setStudent(child);
        when(studentParentRepository.findByParentIdOrderByIdAsc(MY_ID)).thenReturn(List.of(link));
        when(schoolClassRepository.existsByIdAndStudents_Id(MY_CLASS, MY_CHILD)).thenReturn(true);

        assertTrue(service.canViewClassContent(MY_CLASS, auth("ROLE_PARENT")));
    }

    @Test
    void parent_blockedWhenNoChildInClass() {
        StudentParent link = new StudentParent();
        User child = new User();
        child.setId(MY_CHILD);
        link.setStudent(child);
        when(studentParentRepository.findByParentIdOrderByIdAsc(MY_ID)).thenReturn(List.of(link));

        assertFalse(service.canViewClassContent(OTHER_CLASS, auth("ROLE_PARENT")));
    }

    @Test
    void parent_blockedWhenNoChildren() {
        assertFalse(service.canViewClassContent(MY_CLASS, auth("ROLE_PARENT")));
    }

    // ---------------- bien ----------------

    @Test
    void nullAuthentication_blocked() {
        assertFalse(service.canViewClassContent(MY_CLASS, null));
    }

    @Test
    void nullClassId_blocked() {
        assertFalse(service.canViewClassContent(null, auth("ROLE_ADMIN")));
    }

    /**
     * canAccessClass (bao cao CAP LOP) van phai chan STUDENT — hai ham co ngu
     * nghia khac nhau, dung gop lam mot.
     */
    @Test
    void canAccessClass_stillBlocksStudent_unlikeCanViewClassContent() {
        when(schoolClassRepository.existsByIdAndStudents_Id(MY_CLASS, MY_ID)).thenReturn(true);

        assertTrue(service.canViewClassContent(MY_CLASS, auth("ROLE_STUDENT")));
        assertFalse(service.canAccessClass(MY_CLASS, auth("ROLE_STUDENT")));
    }
}
