package com.trungtam.subject.bootstrap;

import com.trungtam.subject.entity.Subject;
import com.trungtam.subject.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Seed 49 mon hoc vao H2 cho profile DEV.
 * O prod, Flyway V6 da seed du lieu nay.
 */
@Slf4j
@Component
@Order(2)
@Profile("dev")
@RequiredArgsConstructor
public class DevSubjectSeeder implements ApplicationRunner {

    private static final String[][] MON_CODES = {
            {"TO", "Toán"},
            {"LY", "Vật lý"},
            {"HO", "Hóa học"},
            {"VA", "Ngữ văn"},
            {"AN", "Tiếng Anh"},
            {"SI", "Sinh học"},
            {"TI", "Tin học"}
    };

    private final SubjectRepository subjectRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (subjectRepository.count() > 0) return;

        List<Subject> subjects = new ArrayList<>();
        for (String[] mon : MON_CODES) {
            for (int khoi = 6; khoi <= 12; khoi++) {
                Subject s = new Subject();
                s.setCode("L" + mon[0] + String.format("%02d", khoi) + "T");
                s.setName(mon[1]);
                s.setGradeLevel("Khối " + khoi);
                subjects.add(s);
            }
        }
        subjectRepository.saveAll(subjects);
        log.info("[DEV] Da seed {} mon hoc (catalog H2 in-memory)", subjects.size());
    }
}
