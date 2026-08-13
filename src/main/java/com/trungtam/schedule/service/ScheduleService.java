package com.trungtam.schedule.service;

import com.trungtam.common.exception.AppException;
import com.trungtam.common.exception.ErrorCode;
import com.trungtam.identity.entity.RoleName;
import com.trungtam.identity.entity.User;
import com.trungtam.guardian.repository.StudentParentRepository;
import com.trungtam.identity.repository.UserRepository;
import com.trungtam.leave.entity.LeaveStatus;
import com.trungtam.leave.repository.LeaveRequestRepository;
import com.trungtam.notification.entity.NotificationType;
import com.trungtam.notification.service.NotificationService;
import com.trungtam.room.entity.Room;
import com.trungtam.room.repository.HolidayRepository;
import com.trungtam.room.repository.RoomRepository;
import com.trungtam.schedule.dto.request.AssignSessionVideosRequest;
import com.trungtam.schedule.dto.request.BulkAssignTopicRequest;
import com.trungtam.schedule.dto.request.CreateManualSessionRequest;
import com.trungtam.schedule.dto.request.CreateScheduleRequest;
import com.trungtam.schedule.dto.request.TimetableQuery;
import com.trungtam.schedule.dto.request.UpdateSessionRequest;
import com.trungtam.schedule.dto.request.UpsertZoomLinkRequest;
import com.trungtam.schedule.dto.response.AttendanceItem;
import com.trungtam.schedule.dto.response.ConflictLine;
import com.trungtam.schedule.dto.response.GeneratePreview;
import com.trungtam.schedule.dto.response.QrTokenResponse;
import com.trungtam.schedule.dto.response.ScheduleItem;
import com.trungtam.schedule.dto.response.SessionDetail;
import com.trungtam.schedule.dto.response.SessionPreviewLine;
import com.trungtam.schedule.dto.response.SessionVideoItem;
import com.trungtam.schedule.dto.response.TimetableItem;
import com.trungtam.schedule.dto.response.ZoomLinkItem;
import com.trungtam.schedule.entity.AttendanceStatus;
import com.trungtam.schedule.entity.ClassSchedule;
import com.trungtam.schedule.entity.ClassSession;
import com.trungtam.schedule.entity.RecurrenceType;
import com.trungtam.schedule.entity.SessionAttendance;
import com.trungtam.schedule.entity.SessionStatus;
import com.trungtam.schedule.entity.SessionVideo;
import com.trungtam.schedule.entity.SessionZoomLink;
import com.trungtam.schedule.repository.ClassRosterRepository;
import com.trungtam.schedule.repository.ClassScheduleRepository;
import com.trungtam.schedule.repository.ClassSessionRepository;
import com.trungtam.schedule.repository.SessionAttendanceRepository;
import com.trungtam.schedule.repository.SessionVideoRepository;
import com.trungtam.schedule.repository.SessionZoomLinkRepository;
import com.trungtam.schoolclass.entity.SchoolClass;
import com.trungtam.schoolclass.repository.SchoolClassRepository;
import com.trungtam.security.SecurityService;
import com.trungtam.security.SecurityUtils;
import com.trungtam.topic.entity.Topic;
import com.trungtam.topic.repository.TopicRepository;
import com.trungtam.video.entity.LectureVideo;
import com.trungtam.video.repository.LectureVideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Quy tac lich, sinh buoi (chong trung), CRUD buoi, diem danh, timetable, QR.
 * Doc ky §3 SPEC_LichHoc_CHITIET.md.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScheduleService {

    private static final long MINUTES_IN_DAY = 24L * 60L;
    private static final String VIEW_STUDENT = "STUDENT";
    private static final String VIEW_TEACHER = "TEACHER";
    private static final String VIEW_ROOM = "ROOM";
    private static final String VIEW_PARENT = "PARENT";
    private static final String DEFAULT_ZOOM_LABEL = "Link chinh";
    private static final Pattern ZOOM_URL = Pattern.compile(
            "^(https?://)?([\\w-]+\\.)?zoom\\.us/(j|my|w|s)/.*$", Pattern.CASE_INSENSITIVE);

    private final ClassScheduleRepository scheduleRepository;
    private final ClassSessionRepository sessionRepository;
    private final SessionAttendanceRepository attendanceRepository;
    private final ClassRosterRepository rosterRepository;
    private final SchoolClassRepository classRepository;
    private final TopicRepository topicRepository;
    private final RoomRepository roomRepository;
    private final HolidayRepository holidayRepository;
    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final StudentParentRepository studentParentRepository;
    private final NotificationService notificationService;
    private final QrTokenService qrTokenService;
    private final TeacherAttendanceService teacherAttendanceService;
    private final SessionVideoRepository sessionVideoRepository;
    private final SessionZoomLinkRepository zoomLinkRepository;
    private final LectureVideoRepository lectureVideoRepository;
    private final SecurityService securityService;

    @Value("${app.schedule.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    @Value("${app.jobs.missing-scan.grace-minutes:15}")
    private int graceMinutes;

    // ============================ CRUD QUY TAC ============================

    public List<ScheduleItem> listSchedules(Long classId) {
        findClassOrThrow(classId);
        return scheduleRepository.findByClazzIdAndActiveTrue(classId).stream()
                .map(ScheduleItem::from)
                .toList();
    }

    @Transactional
    public GeneratePreview createSchedule(Long classId, CreateScheduleRequest req) {
        SchoolClass clazz = findClassOrThrow(classId);
        ClassSchedule schedule = new ClassSchedule();
        schedule.setClazz(clazz);
        applyScheduleRequest(schedule, req);
        scheduleRepository.save(schedule);
        // Sinh buoi ngay sau khi luu quy tac
        return generate(schedule, false);
    }

    @Transactional
    public GeneratePreview updateSchedule(Long id, CreateScheduleRequest req) {
        ClassSchedule schedule = findScheduleOrThrow(id);
        applyScheduleRequest(schedule, req);
        scheduleRepository.save(schedule);
        // Chi dong buoi TUONG LAI, PLANNED, non-manual: xoa truoc roi sinh lai
        LocalDate from = today();
        List<ClassSession> future = sessionRepository.findFutureGeneratedBySchedule(id, from);
        sessionRepository.deleteAll(future);
        sessionRepository.flush();
        return generate(schedule, false);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        ClassSchedule schedule = findScheduleOrThrow(id);
        // Xoa buoi tuong lai PLANNED non-manual cua quy tac; cac buoi khac giu lai (schedule_id -> null do FK SET NULL khi xoa quy tac)
        LocalDate from = today();
        List<ClassSession> future = sessionRepository.findFutureGeneratedBySchedule(id, from);
        sessionRepository.deleteAll(future);
        sessionRepository.flush();
        scheduleRepository.delete(schedule);
    }

    @Transactional
    public GeneratePreview previewSchedule(Long classId, CreateScheduleRequest req) {
        SchoolClass clazz = findClassOrThrow(classId);
        // Quy tac tam (khong luu) de chay dry-run
        ClassSchedule schedule = new ClassSchedule();
        schedule.setClazz(clazz);
        applyScheduleRequest(schedule, req);
        return generate(schedule, true);
    }

    /** Validate + map request -> entity (dung chung create/update). */
    private void applyScheduleRequest(ClassSchedule schedule, CreateScheduleRequest req) {
        if (req.recurrenceType() == RecurrenceType.WEEKLY && req.dayOfWeek() == null) {
            throw new AppException(ErrorCode.WEEKLY_REQUIRES_DAY);
        }
        validateSessionTime(req.startTime(), req.durationMinutes());

        schedule.setRecurrenceType(req.recurrenceType());
        schedule.setDayOfWeek(req.recurrenceType() == RecurrenceType.WEEKLY ? req.dayOfWeek() : null);
        schedule.setStartDate(req.startDate());
        // ONCE -> ep endDate = startDate
        schedule.setEndDate(req.recurrenceType() == RecurrenceType.ONCE
                ? req.startDate()
                : (req.endDate() != null ? req.endDate() : req.startDate()));
        schedule.setStartTime(req.startTime());
        schedule.setDurationMinutes(req.durationMinutes());
        schedule.setRoom(resolveRoom(req.roomId()));
        schedule.setTeacher(resolveTeacher(req.teacherId()));
        schedule.setActive(req.active() == null || req.active());
        String defaultZoomUrl = req.defaultZoomUrl() == null ? null : req.defaultZoomUrl().trim();
        if (defaultZoomUrl != null && !defaultZoomUrl.isEmpty()) {
            validateZoomUrl(defaultZoomUrl);
        } else {
            defaultZoomUrl = null;
        }
        schedule.setDefaultZoomUrl(defaultZoomUrl);
    }

    // ============================ SINH BUOI (§3.6.1) ============================

    /**
     * Sinh buoi tu quy tac. dryRun=true -> preview, khong ghi. dryRun=false -> tao buoi idempotent.
     * Chay trong 1 @Transactional khi ghi.
     */
    @Transactional
    public GeneratePreview generate(ClassSchedule rule, boolean dryRun) {
        LocalDate todayDate = today();
        // Chi sinh tuong lai (khong dong buoi qua khu)
        LocalDate from = rule.getStartDate().isAfter(todayDate) ? rule.getStartDate() : todayDate;
        LocalDate to = rule.getEndDate();

        List<SessionPreviewLine> sessions = new ArrayList<>();
        List<ConflictLine> conflicts = new ArrayList<>();
        if (to == null || from.isAfter(to)) {
            return new GeneratePreview(0, sessions, conflicts);
        }

        // Holiday theo branch cua phong; phong null -> chi holiday toan he thong
        Set<LocalDate> holidays = resolveHolidays(rule.getRoom(), from, to);

        Long classId = rule.getClazz().getId();
        List<Long> roster = rosterRepository.findStudentIdsByClassId(classId);
        LocalTime startTime = rule.getStartTime();
        int duration = rule.getDurationMinutes();
        LocalTime endTime = startTime.plusMinutes(duration);
        Long roomId = rule.getRoom() != null ? rule.getRoom().getId() : null;
        String roomName = rule.getRoom() != null ? rule.getRoom().getName() : null;
        Long teacherId = rule.getTeacher() != null ? rule.getTeacher().getId() : null;
        String teacherName = rule.getTeacher() != null ? rule.getTeacher().getFullName() : null;

        int total = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (!matchesRecurrence(rule, date) || holidays.contains(date)) {
                continue;
            }
            boolean blocked = false;

            // a) trung phong (chan)
            if (roomId != null) {
                List<ClassSession> roomConf = sessionRepository.findRoomConflicts(roomId, date, startTime, duration, null);
                if (!roomConf.isEmpty()) {
                    blocked = true;
                    conflicts.add(new ConflictLine(date, startTime, "ROOM", roomName,
                            roomConf.get(0).getClazz().getName()));
                }
            }
            // b) trung GV (chan)
            if (teacherId != null) {
                List<ClassSession> teacherConf = sessionRepository.findTeacherConflicts(teacherId, date, startTime, duration, null);
                if (!teacherConf.isEmpty()) {
                    blocked = true;
                    conflicts.add(new ConflictLine(date, startTime, "TEACHER", teacherName,
                            teacherConf.get(0).getClazz().getName()));
                }
            }
            // c) trung HV (chi canh bao)
            if (!roster.isEmpty()) {
                List<String> studentConf = sessionRepository.findStudentConflictClassNames(
                        classId, roster, date, startTime, duration);
                for (String otherClass : studentConf) {
                    conflicts.add(new ConflictLine(date, startTime, "STUDENT", null, otherClass));
                }
            }

            sessions.add(new SessionPreviewLine(date, startTime, endTime, roomId, roomName, teacherId, teacherName, blocked));
            total++;

            // Ghi DB khi khong dryRun va khong blocked va chua ton tai (idempotent theo uq)
            if (!dryRun && !blocked
                    && !sessionRepository.existsByClazzIdAndSessionDateAndStartTime(classId, date, startTime)) {
                ClassSession s = new ClassSession();
                s.setClazz(rule.getClazz());
                s.setSchedule(rule.getId() != null ? rule : null);
                s.setSessionDate(date);
                s.setStartTime(startTime);
                s.setDurationMinutes(duration);
                s.setRoom(rule.getRoom());
                s.setTeacher(rule.getTeacher());
                s.setStatus(SessionStatus.PLANNED);
                s.setManual(false);
                // Gia buoi = gia khoa hien hanh (SPEC_ThanhToan §0.2#2/#13)
                s.setPrice(rule.getClazz().getPricePerSession());
                s.setPriceOverridden(false);
                sessionRepository.save(s);
                // Copy link Zoom mac dinh cua quy tac (neu co) vao buoi vua sinh —
                // chi ap dung luc SINH MOI, khong ghi de buoi da co san (SPEC_VideoBaiGiang_Zoom.md §3.3).
                if (rule.getDefaultZoomUrl() != null && !rule.getDefaultZoomUrl().isBlank()) {
                    SessionZoomLink link = new SessionZoomLink();
                    link.setSession(s);
                    link.setLabel(DEFAULT_ZOOM_LABEL);
                    link.setZoomUrl(rule.getDefaultZoomUrl());
                    link.setSortOrder(0);
                    zoomLinkRepository.save(link);
                }
            }
        }
        return new GeneratePreview(total, sessions, conflicts);
    }

    private boolean matchesRecurrence(ClassSchedule rule, LocalDate date) {
        return switch (rule.getRecurrenceType()) {
            case ONCE -> date.equals(rule.getStartDate());
            case DAILY -> true;
            case WEEKLY -> rule.getDayOfWeek() != null
                    && date.getDayOfWeek().getValue() == rule.getDayOfWeek();
        };
    }

    private Set<LocalDate> resolveHolidays(Room room, LocalDate from, LocalDate to) {
        if (room != null && room.getBranch() != null) {
            return new HashSet<>(holidayRepository.findDatesInRange(from, to, room.getBranch().getId()));
        }
        // Khong biet co so -> chi holiday toan he thong (§12.4)
        return new HashSet<>(holidayRepository.findGlobalDatesInRange(from, to));
    }

    // ============================ BUOI HOC (§3.6.3) ============================

    /**
     * Buoi hoc cua 1 lop. {@code from}/{@code to} la TUY CHON:
     * bo trong ca hai = lay TOAN BO buoi cua khoa (che do "Toan khoa" o man
     * gan chuyen de — SPEC_KhoaHoc_NoiDung_Mobile.md §3.4e).
     *
     * <p>Phai re nhanh that su, KHONG duoc truyen null xuong
     * {@code findByClazzIdAndSessionDateBetween}: {@code BETWEEN null AND null}
     * tra ve rong chu khong phai "tat ca" — loi im lang.
     */
    public List<SessionDetail> listSessions(Long classId, LocalDate from, LocalDate to) {
        findClassOrThrow(classId);
        List<ClassSession> sessions = (from == null && to == null)
                ? sessionRepository.findByClazzId(classId)
                : sessionRepository.findByClazzIdAndSessionDateBetween(
                        classId,
                        from != null ? from : LocalDate.of(1970, 1, 1),
                        to != null ? to : LocalDate.of(2999, 12, 31));
        return sessions.stream()
                .sorted((a, b) -> {
                    int c = a.getSessionDate().compareTo(b.getSessionDate());
                    return c != 0 ? c : a.getStartTime().compareTo(b.getStartTime());
                })
                .map(SessionDetail::from)
                .toList();
    }

    public SessionDetail getSession(Long id) {
        return SessionDetail.from(findSessionOrThrow(id));
    }

    /**
     * Gan (hoac go) chuyen de cho nhieu buoi hoc cua cung 1 khoa.
     * {@code topicId = null} => go chuyen de. Tra ve so buoi da cap nhat.
     *
     * <p>TOAN BO-HOAC-KHONG-GI: validate het truoc khi ghi, va ca lo nam trong
     * 1 transaction. Neu co bat ky sessionId nao khong thuoc khoa -> nem loi va
     * KHONG ghi gi ca (nguoi dung can biet thao tac khong tron ven, khong duoc
     * im lang bo qua).
     *
     * <p>Dung {@code saveAll} tren entity da nap chu KHONG dung JPQL bulk
     * update: bulk update se BO QUA audit cua BaseEntity (@LastModifiedBy),
     * mat dau vet ai gan chuyen de. Cac buoi da duoc nap san de validate nen
     * saveAll chi phat sinh cac cau UPDATE theo lo, khong phai N+1.
     */
    @Transactional
    public int bulkAssignTopic(Long classId, BulkAssignTopicRequest req) {
        SchoolClass clazz = findClassOrThrow(classId);

        Topic topic = null;
        if (req.topicId() != null) {
            topic = topicRepository.findById(req.topicId())
                    .orElseThrow(() -> new AppException(ErrorCode.TOPIC_NOT_FOUND));
            Long topicSubjectId = topic.getSubject() != null ? topic.getSubject().getId() : null;
            Long classSubjectId = clazz.getSubject() != null ? clazz.getSubject().getId() : null;
            if (topicSubjectId == null || !topicSubjectId.equals(classSubjectId)) {
                throw new AppException(ErrorCode.TOPIC_SUBJECT_MISMATCH);
            }
        }

        List<Long> ids = req.sessionIds().stream().filter(Objects::nonNull).distinct().toList();
        List<ClassSession> sessions = sessionRepository.findAllById(ids);
        // Thieu id (khong ton tai) HOAC co buoi cua lop khac -> deu la
        // SESSION_NOT_IN_CLASS: tu goc nhin nguoi goi, id do khong thuoc khoa nay.
        if (sessions.size() != ids.size()
                || sessions.stream().anyMatch(s -> !s.getClazz().getId().equals(classId))) {
            throw new AppException(ErrorCode.SESSION_NOT_IN_CLASS);
        }

        for (ClassSession s : sessions) {
            s.setTopic(topic);
        }
        sessionRepository.saveAll(sessions);
        return sessions.size();
    }

    @Transactional
    public SessionDetail updateSession(Long id, UpdateSessionRequest req) {
        ClassSession s = findSessionOrThrow(id);
        if (req.startTime() != null) {
            s.setStartTime(req.startTime());
        }
        if (req.durationMinutes() != null) {
            s.setDurationMinutes(req.durationMinutes());
        }
        if (req.roomId() != null) {
            s.setRoom(resolveRoom(req.roomId()));
        }
        if (req.teacherId() != null) {
            s.setTeacher(resolveTeacher(req.teacherId()));
        }
        // Ten buoi: chi la metadata noi dung, KHONG phai thay doi lich ->
        // khong tinh vao `changed` (khong ban thong bao cho HV) va khong
        // danh dau isManual (xem chu thich ben duoi).
        if (req.title() != null) {
            s.setTitle(req.title().isBlank() ? null : req.title().trim());
        }
        validateSessionTime(s.getStartTime(), s.getDurationMinutes());
        // Kiem trung lai (loai tru chinh no)
        checkConflictsBlocking(s.getRoom() != null ? s.getRoom().getId() : null,
                s.getTeacher() != null ? s.getTeacher().getId() : null,
                s.getSessionDate(), s.getStartTime(), s.getDurationMinutes(), s.getId());
        boolean changed = req.startTime() != null || req.durationMinutes() != null
                || req.roomId() != null || req.teacherId() != null;
        // isManual = true nghia la "admin da chinh tay buoi nay" -> buoi bi LOAI
        // khoi viec sinh lai lich khi sua quy tac (ClassSessionRepository
        // .findFutureGeneratedBySchedule loc isManual = false). Vi vay chi danh
        // dau khi that su doi thong tin LICH; sua moi ten buoi thi khong.
        if (changed) {
            s.setManual(true);
        }
        sessionRepository.save(s);
        if (changed) {
            // Phan dac trung cho lan doi nay -> dedupeKey khac nhau giua cac lan doi.
            String variant = s.getStartTime() + "_" + s.getDurationMinutes() + "_"
                    + (s.getRoom() != null ? s.getRoom().getId() : "-") + "_"
                    + (s.getTeacher() != null ? s.getTeacher().getId() : "-");
            notifyScheduleChanged(s, variant, "Buoi hoc thay doi",
                    "Thong tin buoi hoc (gio/phong/giao vien) da duoc cap nhat.");
        }
        return SessionDetail.from(s);
    }

    @Transactional
    public SessionDetail cancelSession(Long id, String reason) {
        ClassSession s = findSessionOrThrow(id);
        s.setStatus(SessionStatus.CANCELLED);
        s.setCancelReason(reason);
        sessionRepository.save(s);
        notifyScheduleChanged(s, "CANCEL", "Buoi hoc bi huy",
                "Buoi hoc da bi huy" + (reason != null && !reason.isBlank() ? ": " + reason : "") + ".");
        return SessionDetail.from(s);
    }

    /**
     * Phat SCHEDULE_CHANGED cho HV trong roster cua buoi + phu huynh cua ho.
     * variant gop vao dedupeKey de khong chan lan doi sau (vd thay doi lan 2).
     * enqueue chay REQUIRES_NEW nen an toan goi giua giao dich.
     */
    private void notifyScheduleChanged(ClassSession s, String variant, String title, String body) {
        Long sessionId = s.getId();
        String payload = "{\"sessionId\":" + sessionId + "}";
        for (Long studentId : rosterRepository.findStudentIdsByClassId(s.getClazz().getId())) {
            notificationService.enqueue(studentId, NotificationType.SCHEDULE_CHANGED, title, body, payload,
                    "SCHEDULE_CHANGED:" + sessionId + ":" + variant + ":" + studentId);
            for (Long parentId : studentParentRepository.findParentIdsByStudentId(studentId)) {
                notificationService.enqueue(parentId, NotificationType.SCHEDULE_CHANGED, title, body, payload,
                        "SCHEDULE_CHANGED:" + sessionId + ":" + variant + ":" + parentId);
            }
        }
    }

    @Transactional
    public SessionDetail createManualSession(Long classId, CreateManualSessionRequest req) {
        SchoolClass clazz = findClassOrThrow(classId);
        validateSessionTime(req.startTime(), req.durationMinutes());
        Room room = resolveRoom(req.roomId());
        User teacher = resolveTeacher(req.teacherId());
        checkConflictsBlocking(req.roomId(), req.teacherId(),
                req.sessionDate(), req.startTime(), req.durationMinutes(), null);
        ClassSession s = new ClassSession();
        s.setClazz(clazz);
        s.setSessionDate(req.sessionDate());
        s.setStartTime(req.startTime());
        s.setDurationMinutes(req.durationMinutes());
        s.setRoom(room);
        s.setTeacher(teacher);
        s.setStatus(SessionStatus.PLANNED);
        s.setManual(true);
        // Gia buoi = gia khoa hien hanh (SPEC_ThanhToan §0.2#2/#13)
        s.setPrice(clazz.getPricePerSession());
        s.setPriceOverridden(false);
        sessionRepository.save(s);
        return SessionDetail.from(s);
    }

    /** Chan neu trung phong/GV (HV chi canh bao -> bo qua o day). */
    private void checkConflictsBlocking(Long roomId, Long teacherId, LocalDate date,
                                        LocalTime startTime, int duration, Long excludeId) {
        if (roomId != null
                && !sessionRepository.findRoomConflicts(roomId, date, startTime, duration, excludeId).isEmpty()) {
            throw new AppException(ErrorCode.ROOM_TIME_CONFLICT);
        }
        if (teacherId != null
                && !sessionRepository.findTeacherConflicts(teacherId, date, startTime, duration, excludeId).isEmpty()) {
            throw new AppException(ErrorCode.TEACHER_TIME_CONFLICT);
        }
    }

    // ============================ VIDEO / ZOOM (SPEC_VideoBaiGiang_Zoom.md) ============================

    /** Doc: HV/PH chi xem buoi cua lop/con minh — dung lai guard cua §/outline. */
    public List<SessionVideoItem> listSessionVideos(Long sessionId) {
        ClassSession s = findSessionOrThrow(sessionId);
        requireCanViewClassContent(s.getClazz().getId());
        return sessionVideoRepository.findBySessionIdOrderBySortOrderAsc(sessionId).stream()
                .map(SessionVideoItem::from)
                .toList();
    }

    /**
     * Gan (GHI DE toan bo) danh sach video cho 1 buoi hoc, thu tu theo
     * {@code videoIds}. Khong kiem ownership theo GV day buoi — xem
     * SPEC_VideoBaiGiang_Zoom.md §1.1 (nhat quan voi CLASS:WRITE hien tai,
     * chan o tang @PreAuthorize SESSION_CONTENT:WRITE tren controller).
     */
    @Transactional
    public List<SessionVideoItem> assignSessionVideos(Long sessionId, AssignSessionVideosRequest req) {
        ClassSession s = findSessionOrThrow(sessionId);
        List<Long> videoIds = req.videoIds() == null
                ? List.of()
                : req.videoIds().stream().filter(Objects::nonNull).distinct().toList();
        Map<Long, LectureVideo> byId = lectureVideoRepository.findAllById(videoIds).stream()
                .collect(Collectors.toMap(LectureVideo::getId, v -> v));
        if (byId.size() != videoIds.size()) {
            throw new AppException(ErrorCode.VIDEO_NOT_FOUND);
        }
        sessionVideoRepository.deleteBySessionId(sessionId);
        sessionVideoRepository.flush();
        List<SessionVideo> rows = new ArrayList<>();
        int order = 0;
        for (Long videoId : videoIds) {
            SessionVideo sv = new SessionVideo();
            sv.setSession(s);
            sv.setVideo(byId.get(videoId));
            sv.setSortOrder(order++);
            rows.add(sv);
        }
        sessionVideoRepository.saveAll(rows);
        return rows.stream().map(SessionVideoItem::from).toList();
    }

    public List<ZoomLinkItem> listZoomLinks(Long sessionId) {
        ClassSession s = findSessionOrThrow(sessionId);
        requireCanViewClassContent(s.getClazz().getId());
        return zoomLinkRepository.findBySessionIdOrderBySortOrderAsc(sessionId).stream()
                .map(ZoomLinkItem::from)
                .toList();
    }

    @Transactional
    public ZoomLinkItem addZoomLink(Long sessionId, UpsertZoomLinkRequest req) {
        ClassSession s = findSessionOrThrow(sessionId);
        String url = req.zoomUrl().trim();
        validateZoomUrl(url);
        SessionZoomLink link = new SessionZoomLink();
        link.setSession(s);
        link.setLabel(resolveZoomLabel(req.label()));
        link.setZoomUrl(url);
        link.setMeetingId(req.meetingId());
        link.setPasscode(req.passcode());
        link.setSortOrder((int) zoomLinkRepository.countBySessionId(sessionId));
        return ZoomLinkItem.from(zoomLinkRepository.save(link));
    }

    @Transactional
    public ZoomLinkItem updateZoomLink(Long linkId, UpsertZoomLinkRequest req) {
        SessionZoomLink link = findZoomLinkOrThrow(linkId);
        String url = req.zoomUrl().trim();
        validateZoomUrl(url);
        link.setLabel(resolveZoomLabel(req.label()));
        link.setZoomUrl(url);
        link.setMeetingId(req.meetingId());
        link.setPasscode(req.passcode());
        return ZoomLinkItem.from(zoomLinkRepository.save(link));
    }

    @Transactional
    public void deleteZoomLink(Long linkId) {
        SessionZoomLink link = findZoomLinkOrThrow(linkId);
        zoomLinkRepository.delete(link);
    }

    private String resolveZoomLabel(String label) {
        return (label == null || label.isBlank()) ? DEFAULT_ZOOM_LABEL : label.trim();
    }

    private void validateZoomUrl(String url) {
        if (url == null || !ZOOM_URL.matcher(url).matches()) {
            throw new AppException(ErrorCode.ZOOM_URL_INVALID);
        }
    }

    private SessionZoomLink findZoomLinkOrThrow(Long linkId) {
        return zoomLinkRepository.findById(linkId)
                .orElseThrow(() -> new AppException(ErrorCode.ZOOM_LINK_NOT_FOUND));
    }

    private void requireCanViewClassContent(Long classId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!securityService.canViewClassContent(classId, auth)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    // ============================ DIEM DANH (§3.6.4) ============================

    /** Job dau ngay: tao attendance cho roster cua moi buoi PLANNED trong ngay, ap CO_PHEP theo leave. */
    @Transactional
    public int activateAttendance(LocalDate date) {
        int created = 0;
        for (ClassSession s : sessionRepository.findBySessionDateAndStatus(date, SessionStatus.PLANNED)) {
            List<Long> roster = rosterRepository.findStudentIdsByClassId(s.getClazz().getId());
            for (Long userId : roster) {
                if (attendanceRepository.existsBySessionIdAndUserId(s.getId(), userId)) {
                    continue;
                }
                SessionAttendance a = new SessionAttendance();
                a.setSession(s);
                a.setUser(userRepository.getReferenceById(userId));
                boolean onLeave = leaveRequestRepository.isOnLeave(
                        userId, s.getId(), s.getSessionDate(), s.getClazz().getId(), LeaveStatus.APPROVED);
                a.setStatus(onLeave ? AttendanceStatus.CO_PHEP : AttendanceStatus.CHUA_CHECKIN);
                attendanceRepository.save(a);
                created++;
            }
        }
        return created;
    }

    @Transactional
    public void checkin(Long sessionId, String token, Long currentUserId) {
        ClassSession s = findSessionOrThrow(sessionId);
        if (!qrTokenService.isValid(sessionId, token)) {
            throw new AppException(ErrorCode.QR_TOKEN_INVALID);
        }
        requireInRoster(s, currentUserId);
        SessionAttendance a = attendanceRepository.findBySessionIdAndUserId(sessionId, currentUserId)
                .orElseGet(() -> newAttendance(s, currentUserId));
        Instant now = Instant.now();
        a.setCheckInAt(now);
        // TRE neu tre qua (start + grace)
        LocalTime nowLocal = LocalTime.now(zone());
        LocalDate nowDate = LocalDate.now(zone());
        boolean late = nowDate.isAfter(s.getSessionDate())
                || (nowDate.equals(s.getSessionDate())
                    && nowLocal.isAfter(s.getStartTime().plusMinutes(graceMinutes)));
        a.setStatus(late ? AttendanceStatus.TRE : AttendanceStatus.CO_MAT);
        clearConfirmation(a);
        attendanceRepository.save(a);

        String studentName = studentName(currentUserId);
        String when = formatTime(a.getCheckInAt());
        String stt = late ? "tre" : "dung gio";
        String shortBody = studentName + " da check-in luc " + when + " (" + stt + ").";
        String fullContent = shortBody + "\n\n" + describeSession(s);
        notifyParentsAttendance(s, currentUserId, NotificationType.CHECKIN_OK,
                "Da check-in", shortBody, "Con da check-in", fullContent, "CHECKIN_OK");
    }

    @Transactional
    public void checkout(Long sessionId, String token, Long currentUserId) {
        ClassSession s = findSessionOrThrow(sessionId);
        if (!qrTokenService.isValid(sessionId, token)) {
            throw new AppException(ErrorCode.QR_TOKEN_INVALID);
        }
        requireInRoster(s, currentUserId);
        SessionAttendance a = attendanceRepository.findBySessionIdAndUserId(sessionId, currentUserId)
                .orElseGet(() -> {
                    // Chua co dong / chua check-in -> tao + danh dau co mat (bat thuong: checkout truoc checkin)
                    SessionAttendance n = newAttendance(s, currentUserId);
                    n.setStatus(AttendanceStatus.CO_MAT);
                    return n;
                });
        a.setCheckOutAt(Instant.now());
        clearConfirmation(a);
        attendanceRepository.save(a);

        String studentName = studentName(currentUserId);
        String when = formatTime(a.getCheckOutAt());
        String shortBody = studentName + " da check-out luc " + when + ".";
        String fullContent = shortBody + "\n\n" + describeSession(s);
        notifyParentsAttendance(s, currentUserId, NotificationType.CHECKOUT_OK,
                "Da check-out", shortBody, "Con da check-out", fullContent, "CHECKOUT_OK");
    }

    /**
     * Bao phu huynh cua HV khi HV check-in/check-out (idempotent theo sessionId+parentId).
     * Tao Tin nhan (noi dung day du) + Thong bao (vắn tắt) qua notify().
     */
    private void notifyParentsAttendance(ClassSession s, Long studentId, NotificationType type,
                                         String title, String body, String fullTitle,
                                         String fullContent, String keyPrefix) {
        String payload = "{\"sessionId\":" + s.getId() + ",\"studentId\":" + studentId + "}";
        for (Long parentId : studentParentRepository.findParentIdsByStudentId(studentId)) {
            notificationService.notify(parentId, type, title, body, fullTitle, fullContent, payload,
                    keyPrefix + ":" + s.getId() + ":" + parentId);
        }
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private String studentName(Long userId) {
        return userRepository.findById(userId).map(User::getFullName).orElse("Hoc vien");
    }

    private String formatTime(Instant instant) {
        return instant == null ? "" : instant.atZone(zone()).toLocalTime().format(TIME_FMT);
    }

    /** Mo ta buoi hoc (lop, mon, ngay, gio, phong) cho noi dung tin nhan day du. */
    private String describeSession(ClassSession s) {
        SchoolClass c = s.getClazz();
        StringBuilder sb = new StringBuilder();
        sb.append("Lop: ").append(c.getName());
        if (c.getSubject() != null && c.getSubject().getName() != null) {
            sb.append(" (").append(c.getSubject().getName()).append(')');
        }
        sb.append("\nNgay: ").append(s.getSessionDate().format(DATE_FMT));
        sb.append("\nGio: ").append(s.getStartTime().format(TIME_FMT));
        if (s.endTime() != null) {
            sb.append(" - ").append(s.endTime().format(TIME_FMT));
        }
        if (s.getRoom() != null) {
            sb.append("\nPhong: ").append(s.getRoom().getName());
            if (s.getRoom().getBranch() != null) {
                sb.append(" - ").append(s.getRoom().getBranch().getName());
            }
        }
        return sb.toString();
    }

    public List<AttendanceItem> listAttendance(Long sessionId) {
        ClassSession s = findSessionOrThrow(sessionId);
        List<Long> roster = rosterRepository.findStudentIdsByClassId(s.getClazz().getId());
        Map<Long, SessionAttendance> byUser = new HashMap<>();
        for (SessionAttendance a : attendanceRepository.findBySessionId(sessionId)) {
            byUser.put(a.getUser().getId(), a);
        }
        List<AttendanceItem> result = new ArrayList<>();
        for (Long userId : roster) {
            User u = userRepository.findById(userId).orElse(null);
            if (u == null) {
                continue;
            }
            SessionAttendance a = byUser.get(userId);
            result.add(new AttendanceItem(
                    u.getId(), u.getFullName(), u.getUsername(), u.getPhone(),
                    a != null ? a.getStatus() : AttendanceStatus.CHUA_CHECKIN,
                    a != null ? a.getCheckInAt() : null,
                    a != null ? a.getCheckOutAt() : null,
                    a != null && a.getConfirmedBy() != null ? a.getConfirmedBy().getFullName() : null,
                    a != null ? a.getConfirmedAt() : null));
        }
        result.sort((x, y) -> {
            String nx = x.fullName() != null ? x.fullName() : "";
            String ny = y.fullName() != null ? y.fullName() : "";
            return nx.compareTo(ny);
        });
        return result;
    }

    @Transactional
    public void setAttendance(Long sessionId, Long userId, AttendanceStatus status) {
        ClassSession s = findSessionOrThrow(sessionId);
        requireInRoster(s, userId);
        SessionAttendance a = attendanceRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseGet(() -> newAttendance(s, userId));
        a.setStatus(status);
        clearConfirmation(a);
        attendanceRepository.save(a);
    }

    /**
     * GV/Admin/nhan vien xac nhan diem danh dung — BAT BUOC truoc khi tinh
     * vao bao cao (yeu cau nguoi dung 13/08/2026). Xem {@link #clearConfirmation}
     * cho quy tac reset khi doi trang thai.
     */
    @Transactional
    public void confirmAttendance(Long sessionId, Long userId) {
        SessionAttendance a = attendanceRepository.findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTENDANCE_NOT_FOUND));
        if (a.getConfirmedAt() != null) {
            throw new AppException(ErrorCode.ATTENDANCE_ALREADY_CONFIRMED);
        }
        a.setConfirmedBy(userRepository.getReferenceById(currentUserId()));
        a.setConfirmedAt(Instant.now());
        attendanceRepository.save(a);
    }

    private void clearConfirmation(SessionAttendance a) {
        a.setConfirmedBy(null);
        a.setConfirmedAt(null);
    }

    private SessionAttendance newAttendance(ClassSession s, Long userId) {
        SessionAttendance a = new SessionAttendance();
        a.setSession(s);
        a.setUser(userRepository.getReferenceById(userId));
        a.setStatus(AttendanceStatus.CHUA_CHECKIN);
        return a;
    }

    private void requireInRoster(ClassSession s, Long userId) {
        if (!rosterRepository.findStudentIdsByClassId(s.getClazz().getId()).contains(userId)) {
            throw new AppException(ErrorCode.NOT_IN_ROSTER);
        }
    }

    /** Check-in cho chinh nguoi dang dang nhap (resolve user id tu context). */
    @Transactional
    public void checkinSelf(Long sessionId, String token) {
        checkin(sessionId, token, currentUserId());
    }

    /** Check-out cho chinh nguoi dang dang nhap. */
    @Transactional
    public void checkoutSelf(Long sessionId, String token) {
        checkout(sessionId, token, currentUserId());
    }

    private Long currentUserId() {
        String username = SecurityUtils.requireCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
                .getId();
    }

    // ============================ QR TOKEN (§3.8) ============================

    public QrTokenResponse getQrToken(Long sessionId) {
        findSessionOrThrow(sessionId);
        return qrTokenService.currentToken(sessionId);
    }

    // ============================ TIMETABLE (§3.6.5) ============================

    /** Resolve nguoi dung dang dang nhap roi dung quyen so huu. */
    public List<TimetableItem> timetable(TimetableQuery query) {
        String username = SecurityUtils.requireCurrentUsername();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return timetable(query, currentUser);
    }

    public List<TimetableItem> timetable(TimetableQuery query, User currentUser) {
        String view = query.getView() != null ? query.getView().toUpperCase() : "";
        Long refId = resolveRefId(view, query.getRefId(), currentUser);
        LocalDate from = query.getFrom();
        LocalDate to = query.getTo();

        List<TimetableItem> items = switch (view) {
            case VIEW_STUDENT -> studentTimetable(refId, from, to, null, null);
            case VIEW_TEACHER -> teacherTimetable(refId, from, to);
            case VIEW_ROOM -> mapSessions(
                    sessionRepository.findTimetableForRoom(refId, from, to), null, null);
            case VIEW_PARENT -> parentTimetable(refId, from, to);
            default -> throw new AppException(ErrorCode.VALIDATION_ERROR, "view khong hop le");
        };
        return filterByBranch(items, query.getBranchId());
    }

    private List<TimetableItem> studentTimetable(Long studentId, LocalDate from, LocalDate to,
                                                 Long overrideStudentId, String overrideStudentName) {
        List<ClassSession> sessions = sessionRepository.findTimetableForStudent(studentId, from, to);
        // attendance cua HV trong khoang
        Map<Long, SessionAttendance> attBySession = new HashMap<>();
        for (SessionAttendance a : attendanceRepository.findByUserIdAndDateRange(studentId, from, to)) {
            attBySession.put(a.getSession().getId(), a);
        }
        Long stId = overrideStudentId != null ? overrideStudentId : studentId;
        String stName = overrideStudentName;
        if (stName == null) {
            stName = userRepository.findById(studentId).map(User::getFullName).orElse(null);
        }
        List<TimetableItem> result = new ArrayList<>();
        for (ClassSession s : sessions) {
            SessionAttendance a = attBySession.get(s.getId());
            boolean onLeave = leaveRequestRepository.isOnLeave(
                    studentId, s.getId(), s.getSessionDate(), s.getClazz().getId(), LeaveStatus.APPROVED);
            result.add(toItem(s, stId, stName,
                    a != null ? a.getStatus().name() : AttendanceStatus.CHUA_CHECKIN.name(),
                    onLeave));
        }
        return result;
    }

    private List<TimetableItem> teacherTimetable(Long teacherId, LocalDate from, LocalDate to) {
        List<ClassSession> sessions = sessionRepository.findTimetableForTeacher(teacherId, from, to);
        List<Long> ids = sessions.stream().map(ClassSession::getId).toList();
        Map<Long, String> statusMap = teacherAttendanceService.statusBySessionIds(ids);
        List<TimetableItem> result = new ArrayList<>();
        for (ClassSession s : sessions) {
            result.add(toItem(s, null, null, null, false, statusMap.get(s.getId())));
        }
        return result;
    }

    private List<TimetableItem> parentTimetable(Long parentId, LocalDate from, LocalDate to) {
        List<TimetableItem> result = new ArrayList<>();
        // Cac con cua PH lay tu student_parents qua query trong guardian module (neu co).
        // Tranh phu thuoc cung chieu: doc qua native helper roster theo parent.
        for (Long childId : findChildrenIds(parentId)) {
            String childName = userRepository.findById(childId).map(User::getFullName).orElse(null);
            result.addAll(studentTimetable(childId, from, to, childId, childName));
        }
        return result;
    }

    /** Con cua phu huynh tu bang student_parents (doc native, khong phu thuoc module guardian). */
    private List<Long> findChildrenIds(Long parentId) {
        return rosterRepository.findChildrenIdsByParentId(parentId);
    }

    private List<TimetableItem> mapSessions(List<ClassSession> sessions, Long studentId, String studentName) {
        List<TimetableItem> result = new ArrayList<>();
        for (ClassSession s : sessions) {
            result.add(toItem(s, studentId, studentName, null, false, null));
        }
        return result;
    }

    private TimetableItem toItem(ClassSession s, Long studentId, String studentName,
                                 String attendanceStatus, boolean onLeave) {
        return toItem(s, studentId, studentName, attendanceStatus, onLeave, null);
    }

    private TimetableItem toItem(ClassSession s, Long studentId, String studentName,
                                 String attendanceStatus, boolean onLeave,
                                 String teacherAttendanceStatus) {
        SchoolClass c = s.getClazz();
        Room room = s.getRoom();
        return new TimetableItem(
                s.getId(),
                c.getId(),
                c.getName(),
                c.getSubject() != null ? c.getSubject().getName() : null,
                c.getSubject() != null ? c.getSubject().getGradeLevel() : null,
                s.getSessionDate(),
                s.getStartTime(),
                s.endTime(),
                room != null ? room.getId() : null,
                room != null ? room.getName() : null,
                room != null && room.getBranch() != null ? room.getBranch().getName() : null,
                s.getTeacher() != null ? s.getTeacher().getId() : null,
                s.getTeacher() != null ? s.getTeacher().getFullName() : null,
                s.getStatus(),
                studentId,
                studentName,
                attendanceStatus,
                onLeave,
                teacherAttendanceStatus);
    }

    private List<TimetableItem> filterByBranch(List<TimetableItem> items, Long branchId) {
        if (branchId == null) {
            return items;
        }
        // Loc theo branch cua phong: chi giu buoi co phong thuoc branch (suy ra qua room).
        Set<Long> roomIdsInBranch = new HashSet<>();
        for (Room r : roomRepository.findByBranchIdAndActiveTrueOrderByNameAsc(branchId)) {
            roomIdsInBranch.add(r.getId());
        }
        return items.stream()
                .filter(i -> i.roomId() != null && roomIdsInBranch.contains(i.roomId()))
                .toList();
    }

    /**
     * Ep quyen so huu (§3.6.5, §12.10):
     * - ADMIN/EMPLOYEE (CLASS:READ quan ly): refId tuy y; ROOM bat buoc co refId.
     * - TEACHER: chi refId = chinh minh (mac dinh chinh minh neu null).
     * - STUDENT view: chi xem chinh minh.
     * - PARENT view: refId la id cua PHU HUYNH; chi cho phep chinh minh (con lay tu student_parents).
     * - PARENT/STUDENT khong duoc dung view ROOM/TEACHER cua nguoi khac.
     */
    private Long resolveRefId(String view, Long refId, User currentUser) {
        boolean privileged = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ADMIN || r.getName() == RoleName.EMPLOYEE);
        if (privileged) {
            if (VIEW_ROOM.equals(view) && refId == null) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "refId la bat buoc cho view ROOM");
            }
            return refId;
        }
        Long selfId = currentUser.getId();
        return switch (view) {
            case VIEW_STUDENT, VIEW_TEACHER, VIEW_PARENT -> {
                // refId null -> mac dinh chinh minh; neu chi dinh khac chinh minh -> chan
                if (refId != null && !refId.equals(selfId)) {
                    throw new AppException(ErrorCode.ACCESS_DENIED);
                }
                yield selfId;
            }
            case VIEW_ROOM -> throw new AppException(ErrorCode.ACCESS_DENIED);
            default -> throw new AppException(ErrorCode.VALIDATION_ERROR, "view khong hop le");
        };
    }

    // ============================ HELPERS ============================

    private void validateSessionTime(LocalTime startTime, Integer durationMinutes) {
        if (startTime == null || durationMinutes == null || durationMinutes <= 0) {
            throw new AppException(ErrorCode.SESSION_TIME_INVALID);
        }
        long endMinutes = (long) startTime.getHour() * 60 + startTime.getMinute() + durationMinutes;
        if (endMinutes > MINUTES_IN_DAY) {
            throw new AppException(ErrorCode.SESSION_TIME_INVALID);
        }
    }

    private Room resolveRoom(Long roomId) {
        if (roomId == null) {
            return null;
        }
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private User resolveTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        boolean isTeacher = teacher.getRoles().stream().anyMatch(r -> r.getName() == RoleName.TEACHER);
        if (!isTeacher) {
            throw new AppException(ErrorCode.NOT_A_TEACHER);
        }
        return teacher;
    }

    private ZoneId zone() {
        return ZoneId.of(timezone);
    }

    private LocalDate today() {
        return LocalDate.now(zone());
    }

    private SchoolClass findClassOrThrow(Long classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_NOT_FOUND));
    }

    private ClassSchedule findScheduleOrThrow(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SCHEDULE_NOT_FOUND));
    }

    private ClassSession findSessionOrThrow(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
    }

    /** Tien ich cho job sinh buoi mo rong chan troi (SessionGenerationJob). */
    @Transactional
    public int generateHorizon(int horizonWeeks) {
        int count = 0;
        for (ClassSchedule rule : scheduleRepository.findByActiveTrue()) {
            GeneratePreview p = generate(rule, false);
            count += p.total();
        }
        return count;
    }
}
