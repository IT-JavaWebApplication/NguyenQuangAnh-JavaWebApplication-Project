package com.smartacademic.service.impl;

import com.smartacademic.config.HibernateSessionProvider;
import com.smartacademic.dto.stats.DashboardStatsDTO;
import com.smartacademic.dto.stats.EquipmentUsageDTO;
import com.smartacademic.dto.stats.MonthlySessionDTO;
import com.smartacademic.dto.stats.TopLecturerDTO;
import com.smartacademic.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private HibernateSessionProvider sessionFactory;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        var s = sessionFactory.getCurrentSession();

        long totalUsers      = countQuery("SELECT COUNT(u) FROM User u WHERE u.isActive = true");
        long totalStudents   = countQuery("SELECT COUNT(u) FROM User u WHERE u.role = 'STUDENT'  AND u.isActive = true");
        long totalLecturers  = countQuery("SELECT COUNT(u) FROM User u WHERE u.role = 'LECTURER' AND u.isActive = true");
        long totalEquipments = countQuery("SELECT COUNT(e) FROM Equipment e WHERE e.isActive = true");
        long pendingDispatch = countQuery("SELECT COUNT(br) FROM BorrowingRecord br WHERE br.status = 'PENDING_DISPATCH'");
        long overdueCount    = countQuery("SELECT COUNT(br) FROM BorrowingRecord br WHERE br.status = 'OVERDUE'");

        long totalSessions     = countQuery("SELECT COUNT(ms) FROM MentoringSession ms");
        long completedSessions = countQuery("SELECT COUNT(ms) FROM MentoringSession ms WHERE ms.status = 'COMPLETED'");
        long pendingSessions   = countQuery("SELECT COUNT(ms) FROM MentoringSession ms WHERE ms.status = 'PENDING'");

        Long borrowedQty = s.createQuery(
                "SELECT COALESCE(SUM(d.quantity - COALESCE(d.returnQuantity, 0)), 0) " +
                        "FROM BorrowingDetail d " +
                        "WHERE d.borrowingRecord.status IN ('DISPATCHED','OVERDUE')", Long.class)
                .uniqueResult();

        Double avgSkill = s.createQuery(
                "SELECT AVG(e.skillScore) FROM AcademicEvaluation e", Double.class).uniqueResult();
        Double avgAttitude = s.createQuery(
                "SELECT AVG(e.attitudeScore) FROM AcademicEvaluation e", Double.class).uniqueResult();

        DashboardStatsDTO dto = new DashboardStatsDTO();
        dto.setTotalUsers(totalUsers);
        dto.setTotalStudents(totalStudents);
        dto.setTotalLecturers(totalLecturers);
        dto.setTotalEquipments(totalEquipments);
        dto.setTotalEquipmentsBorrowed(borrowedQty != null ? borrowedQty : 0L);
        dto.setPendingDispatchCount(pendingDispatch);
        dto.setOverdueCount(overdueCount);
        dto.setTotalSessions(totalSessions);
        dto.setCompletedSessions(completedSessions);
        dto.setPendingSessions(pendingSessions);
        dto.setAvgSkillScore(avgSkill != null ? Math.round(avgSkill * 10.0) / 10.0 : 0.0);
        dto.setAvgAttitudeScore(avgAttitude != null ? Math.round(avgAttitude * 10.0) / 10.0 : 0.0);

        dto.setTopLecturers(getTopLecturers(5));
        dto.setTopEquipments(getTopBorrowedEquipments(5));
        dto.setSessionsByMonth(getSessionsByMonth(6));

        if (dto.getTopLecturers() == null) {
            dto.setTopLecturers(List.of());
        }
        if (dto.getTopEquipments() == null) {
            dto.setTopEquipments(List.of());
        }
        if (dto.getSessionsByMonth() == null) {
            dto.setSessionsByMonth(List.of());
        }

        return dto;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<TopLecturerDTO> getTopLecturers(int limit) {
        var s = sessionFactory.getCurrentSession();
        List<Object[]> rows = s.createQuery(
                "SELECT u.id, p.fullName, dep.name, " +
                        "       COUNT(ms.id), " +
                        "       COALESCE(AVG(ae.skillScore), 0) " +
                        "FROM User u " +
                        "JOIN u.profile p " +
                        "JOIN u.lecturerInfo li " +
                        "JOIN li.department dep " +
                        "LEFT JOIN MentoringSession ms ON ms.lecturer.id = u.id " +
                        "LEFT JOIN AcademicEvaluation ae ON ae.lecturer.id = u.id " +
                        "WHERE u.role = 'LECTURER' AND u.isActive = true " +
                        "GROUP BY u.id, p.fullName, dep.name " +
                        "ORDER BY COUNT(ms.id) DESC, p.fullName ASC", Object[].class)
                .setMaxResults(limit)
                .list();

        List<TopLecturerDTO> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(new TopLecturerDTO(
                    (Long) r[0],
                    (String) r[1],
                    (String) r[2],
                    ((Number) r[3]).longValue(),
                    Math.round(((Number) r[4]).doubleValue() * 10.0) / 10.0
            ));
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EquipmentUsageDTO> getTopBorrowedEquipments(int limit) {
        var s = sessionFactory.getCurrentSession();
        List<Object[]> rows = s.createQuery(
                "SELECT e.id, e.code, e.name, " +
                        "       COALESCE(SUM(d.quantity), 0) as total, " +
                        "       e.available, e.quantity " +
                        "FROM Equipment e " +
                        "LEFT JOIN BorrowingDetail d ON d.equipment.id = e.id " +
                        "WHERE e.isActive = true " +
                        "GROUP BY e.id, e.code, e.name, e.available, e.quantity " +
                        "ORDER BY COALESCE(SUM(d.quantity), 0) DESC, e.name ASC", Object[].class)
                .setMaxResults(limit)
                .list();

        List<EquipmentUsageDTO> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(new EquipmentUsageDTO(
                    (Long) r[0],
                    (String) r[1],
                    (String) r[2],
                    ((Number) r[3]).longValue(),
                    r[4] == null ? 0 : ((Number) r[4]).intValue(),
                    r[5] == null ? 0 : ((Number) r[5]).intValue()
            ));
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<MonthlySessionDTO> getSessionsByMonth(int monthsBack) {
        var s = sessionFactory.getCurrentSession();
        LocalDate since = LocalDate.now().minusMonths(monthsBack);

        List<Object[]> rows = s.createQuery(
                "SELECT YEAR(ms.sessionDate), MONTH(ms.sessionDate), " +
                        "       COUNT(ms.id), " +
                        "       SUM(CASE WHEN ms.status = 'COMPLETED' THEN 1 ELSE 0 END) " +
                        "FROM MentoringSession ms " +
                        "WHERE ms.sessionDate >= :since " +
                        "GROUP BY YEAR(ms.sessionDate), MONTH(ms.sessionDate) " +
                        "ORDER BY YEAR(ms.sessionDate), MONTH(ms.sessionDate)", Object[].class)
                .setParameter("since", since)
                .list();

        List<MonthlySessionDTO> result = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            result.add(new MonthlySessionDTO(
                    ((Number) r[0]).intValue(),
                    ((Number) r[1]).intValue(),
                    ((Number) r[2]).longValue(),
                    r[3] == null ? 0L : ((Number) r[3]).longValue()
            ));
        }
        return result;
    }

    private long countQuery(String hql) {
        Long n = sessionFactory.getCurrentSession()
                .createQuery(hql, Long.class)
                .uniqueResult();
        return n == null ? 0L : n;
    }
}
