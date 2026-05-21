package com.smartacademic.config;

import com.smartacademic.entity.*;
import com.smartacademic.enums.BorrowingStatus;
import com.smartacademic.enums.Role;
import com.smartacademic.enums.SessionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Seed các user / lecturer / mentoring session / evaluation / borrowing data demo.
 * Phải chạy sau khi data.sql đã insert departments + equipments.
 *
 * Mật khẩu mặc định cho tất cả user demo: <b>password123</b>
 */
@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEFAULT_PASSWORD = "password123";

    @Autowired
    private HibernateSessionProvider sessionFactory;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        var session = sessionFactory.getCurrentSession();

        Long userCount = session.createQuery("SELECT COUNT(u) FROM User u", Long.class)
                .uniqueResult();
        if (userCount != null && userCount > 0) {
            log.info("[DataSeeder] Đã có {} user trong DB, bỏ qua seed.", userCount);
            return;
        }

        log.info("[DataSeeder] Bắt đầu seed dữ liệu user/session/borrowing...");

        String hashed = passwordEncoder.encode(DEFAULT_PASSWORD);

        // ---- USERS + PROFILES ----
        User admin     = createUser(session, "admin",      "admin@smartacademic.edu.vn",     hashed, Role.ADMIN,    "System Administrator", "0909999999", null,         null);
        User lec1      = createUser(session, "lecturer01", "lecturer01@smartacademic.edu.vn", hashed, Role.LECTURER, "TS. Nguyễn Văn An",    "0901234567", null,         null);
        User lec2      = createUser(session, "lecturer02", "lecturer02@smartacademic.edu.vn", hashed, Role.LECTURER, "ThS. Trần Thị Bình",   "0901234568", null,         null);
        User lec3      = createUser(session, "lecturer03", "lecturer03@smartacademic.edu.vn", hashed, Role.LECTURER, "PGS.TS. Lê Văn Cường", "0901234569", null,         null);
        User stu1      = createUser(session, "student01",  "student01@smartacademic.edu.vn",  hashed, Role.STUDENT,  "Phạm Thị Dung",        "0987654321", "SV20260001", "K26-CNTT01");
        User stu2      = createUser(session, "student02",  "student02@smartacademic.edu.vn",  hashed, Role.STUDENT,  "Hoàng Minh Tâm",       "0987654322", "SV20260002", "K26-CNTT01");
        User stu3      = createUser(session, "student03",  "student03@smartacademic.edu.vn",  hashed, Role.STUDENT,  "Vũ Thanh Hằng",        "0987654323", "SV20260003", "K26-KTPM01");

        // ---- LECTURERS info ----
        Department dCntt = session.get(Department.class, 1L);
        Department dKtpm = session.get(Department.class, 3L);
        Department dKhmt = session.get(Department.class, 4L);

        createLecturer(session, lec1, dCntt != null ? dCntt : fallbackDept(session),
                "GV2026001", "Lập trình Java & Spring Boot",
                "Tiến sĩ chuyên ngành Kỹ nghệ phần mềm, hơn 10 năm kinh nghiệm.");
        createLecturer(session, lec2, dKtpm != null ? dKtpm : fallbackDept(session),
                "GV2026002", "Kiểm thử phần mềm & DevOps",
                "Thạc sĩ Kỹ thuật phần mềm, chuyên gia QA - CI/CD.");
        createLecturer(session, lec3, dKhmt != null ? dKhmt : fallbackDept(session),
                "GV2026003", "Trí tuệ nhân tạo & Học máy",
                "PGS.TS chuyên ngành Khoa học máy tính, nghiên cứu AI/ML.");

        // ---- MENTORING SESSIONS ----
        MentoringSession s1 = createSession(session, stu1, lec1, LocalDate.now().minusDays(10), LocalTime.of(8, 0),  LocalTime.of(10, 0),
                SessionStatus.COMPLETED, "Tư vấn Spring Boot REST API");
        MentoringSession s2 = createSession(session, stu1, lec3, LocalDate.now().minusDays(7),  LocalTime.of(13, 30), LocalTime.of(15, 0),
                SessionStatus.COMPLETED, "Tư vấn project AI nhận diện ảnh");
        MentoringSession s3 = createSession(session, stu2, lec1, LocalDate.now().minusDays(5),  LocalTime.of(9, 0),  LocalTime.of(11, 0),
                SessionStatus.COMPLETED, "Review project Hibernate");
        createSession(session, stu2, lec2, LocalDate.now().plusDays(2),  LocalTime.of(14, 0), LocalTime.of(16, 0),
                SessionStatus.PENDING,   "Hỏi về unit testing JUnit 5");
        createSession(session, stu3, lec3, LocalDate.now().plusDays(4),  LocalTime.of(8, 30), LocalTime.of(10, 0),
                SessionStatus.PENDING,   "Tư vấn đồ án tốt nghiệp AI");
        createSession(session, stu1, lec2, LocalDate.now().plusDays(6),  LocalTime.of(15, 0), LocalTime.of(16, 30),
                SessionStatus.PENDING,   "CI/CD pipeline với Jenkins");

        // ---- EVALUATIONS ----
        createEvaluation(session, s1, 9, 10,
                "Sinh viên nắm vững kiến thức Spring Boot, code clean.",
                "Nghiên cứu thêm Spring Cloud và Microservices.");
        createEvaluation(session, s2, 8, 9,
                "Hiểu tốt về CNN, đã train được mô hình.",
                "Tiếp tục với Transfer Learning, PyTorch.");
        createEvaluation(session, s3, 7, 8,
                "Code chạy được nhưng cần cải thiện performance.",
                "Tối ưu N+1 query, dùng @EntityGraph.");

        // ---- BORROWING RECORDS + DETAILS ----
        Equipment pc      = session.get(Equipment.class, 1L);
        Equipment bookJ   = session.get(Equipment.class, 7L);
        Equipment bookDb  = session.get(Equipment.class, 8L);
        Equipment pi      = session.get(Equipment.class, 5L);
        Equipment jetson  = session.get(Equipment.class, 10L);

        if (pc != null && bookJ != null) {
            BorrowingRecord br1 = createBorrowing(session, s1, BorrowingStatus.DISPATCHED,
                    LocalDate.now().plusDays(7), "Đã bàn giao thiết bị thực hành Spring Boot");
            br1.setDispatchedAt(java.time.LocalDateTime.now().minusDays(8));
            addDetail(session, br1, pc, 1);
            addDetail(session, br1, bookJ, 1);
        }
        if (pi != null && jetson != null) {
            BorrowingRecord br2 = createBorrowing(session, s2, BorrowingStatus.PENDING_DISPATCH,
                    LocalDate.now().plusDays(7), null);
            addDetail(session, br2, pi, 1);
            addDetail(session, br2, jetson, 1);
        }
        if (pc != null && bookDb != null) {
            BorrowingRecord br3 = createBorrowing(session, s3, BorrowingStatus.RETURNED,
                    LocalDate.now().minusDays(1), "Đã nhận lại đầy đủ");
            br3.setDispatchedAt(java.time.LocalDateTime.now().minusDays(4));
            br3.setReturnedAt(java.time.LocalDateTime.now().minusDays(1));
            addDetail(session, br3, pc, 1);
            addDetail(session, br3, bookDb, 1);
        }

        log.info("[DataSeeder] Seed dữ liệu hoàn tất. Tài khoản mặc định:");
        log.info("[DataSeeder]   admin/password123, lecturer01..03/password123, student01..03/password123");
    }

    private User createUser(org.hibernate.Session session, String username, String email, String hashed,
                            Role role, String fullName, String phone, String studentCode, String className) {
        User u = new User(username, email, hashed, role);
        session.persist(u);

        UserProfile p = new UserProfile();
        p.setUser(u);
        p.setFullName(fullName);
        p.setPhone(phone);
        p.setStudentCode(studentCode);
        p.setClassName(className);
        session.persist(p);
        return u;
    }

    private void createLecturer(org.hibernate.Session session, User user, Department dept, String code,
                                String specialization, String bio) {
        Lecturer l = new Lecturer();
        l.setUser(user);
        l.setDepartment(dept);
        l.setLecturerCode(code);
        l.setSpecialization(specialization);
        l.setBio(bio);
        l.setSessionFee(new java.math.BigDecimal("100000")); // 100,000 VND/buổi
        session.persist(l);
    }

    private MentoringSession createSession(org.hibernate.Session session, User student, User lecturer,
                                           LocalDate date, LocalTime start, LocalTime end,
                                           SessionStatus status, String note) {
        MentoringSession ms = new MentoringSession();
        ms.setStudent(student);
        ms.setLecturer(lecturer);
        ms.setSessionDate(date);
        ms.setStartTime(start);
        ms.setEndTime(end);
        ms.setStatus(status);
        ms.setNote(note);
        session.persist(ms);
        return ms;
    }

    private void createEvaluation(org.hibernate.Session session, MentoringSession ms, int skill, int attitude,
                                  String comments, String recommendations) {
        AcademicEvaluation ev = new AcademicEvaluation();
        ev.setSession(ms);
        ev.setLecturer(ms.getLecturer());
        ev.setStudent(ms.getStudent());
        ev.setSkillScore(skill);
        ev.setAttitudeScore(attitude);
        ev.setComments(comments);
        ev.setRecommendations(recommendations);
        session.persist(ev);
    }

    private BorrowingRecord createBorrowing(org.hibernate.Session session, MentoringSession ms,
                                            BorrowingStatus status, LocalDate dueDate, String adminNote) {
        BorrowingRecord br = new BorrowingRecord();
        br.setSession(ms);
        br.setStudent(ms.getStudent());
        br.setStatus(status);
        br.setDueDate(dueDate);
        br.setAdminNote(adminNote);
        session.persist(br);
        return br;
    }

    private void addDetail(org.hibernate.Session session, BorrowingRecord br, Equipment eq, int qty) {
        BorrowingDetail d = new BorrowingDetail(br, eq, qty);
        if (br.getStatus() == BorrowingStatus.RETURNED) {
            d.setReturnQuantity(qty);
        }
        session.persist(d);
    }

    private Department fallbackDept(org.hibernate.Session session) {
        List<Department> all = session.createQuery("FROM Department d", Department.class).list();
        if (all.isEmpty()) {
            Department d = new Department();
            d.setCode("DEFAULT");
            d.setName("Khoa mặc định");
            d.setIsActive(true);
            session.persist(d);
            return d;
        }
        return all.get(0);
    }
}
