package com.smartacademic.repository.impl;

import com.smartacademic.config.HibernateSessionProvider;
import com.smartacademic.entity.MentoringSession;
import com.smartacademic.enums.SessionStatus;
import com.smartacademic.repository.MentoringSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MentoringSessionRepositoryImpl implements MentoringSessionRepository {

    @Autowired
    private HibernateSessionProvider sessionFactory;

    @Override
    public MentoringSession save(MentoringSession session) {
        sessionFactory.getCurrentSession().persist(session);
        return session;
    }

    @Override
    public Optional<MentoringSession> findById(Long id) {
        MentoringSession session = sessionFactory.getCurrentSession().get(MentoringSession.class, id);
        return Optional.ofNullable(session);
    }

    @Override
    public List<MentoringSession> findByStudentId(Long studentId) {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT ms FROM MentoringSession ms " +
                                "LEFT JOIN FETCH ms.lecturer l " +
                                "LEFT JOIN FETCH l.profile " +
                                "WHERE ms.student.id = :studentId " +
                                "ORDER BY ms.sessionDate DESC, ms.startTime DESC", MentoringSession.class)
                .setParameter("studentId", studentId)
                .list();
    }

    @Override
    public List<MentoringSession> findByLecturerId(Long lecturerId) {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT ms FROM MentoringSession ms " +
                                "LEFT JOIN FETCH ms.student s " +
                                "LEFT JOIN FETCH s.profile " +
                                "WHERE ms.lecturer.id = :lecturerId " +
                                "ORDER BY ms.sessionDate DESC, ms.startTime DESC", MentoringSession.class)
                .setParameter("lecturerId", lecturerId)
                .list();
    }

    @Override
    public List<MentoringSession> findByLecturerIdAndStatus(Long lecturerId, SessionStatus status) {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT ms FROM MentoringSession ms " +
                                "LEFT JOIN FETCH ms.student s " +
                                "LEFT JOIN FETCH s.profile " +
                                "WHERE ms.lecturer.id = :lecturerId AND ms.status = :status " +
                                "ORDER BY ms.sessionDate ASC, ms.startTime ASC", MentoringSession.class)
                .setParameter("lecturerId", lecturerId)
                .setParameter("status", status)
                .list();
    }

    @Override
    public boolean isSlotTaken(Long lecturerId, LocalDate date, LocalTime startTime) {
        // Kiểm tra xung đột khung giờ.
        Long count = sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT COUNT(ms) FROM MentoringSession ms " +
                                "WHERE ms.lecturer.id = :lecturerId " +
                                "AND ms.sessionDate = :date " +
                                "AND ms.startTime = :startTime " +
                                "AND ms.status IN ('PENDING_PAYMENT', 'PENDING', 'CONFIRMED')", Long.class)
                .setParameter("lecturerId", lecturerId)
                .setParameter("date", date)
                .setParameter("startTime", startTime)
                .uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public void update(MentoringSession session) {
        sessionFactory.getCurrentSession().merge(session);
    }

    @Override
    public List<MentoringSession> findAllPending() {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT ms FROM MentoringSession ms " +
                                "LEFT JOIN FETCH ms.student s " +
                                "LEFT JOIN FETCH s.profile " +
                                "LEFT JOIN FETCH ms.lecturer l " +
                                "LEFT JOIN FETCH l.profile " +
                                "WHERE ms.status = 'PENDING' " +
                                "ORDER BY ms.sessionDate ASC", MentoringSession.class)
                .list();
    }
}