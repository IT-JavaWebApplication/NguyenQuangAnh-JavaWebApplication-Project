package com.smartacademic.repository.impl;

import com.smartacademic.config.HibernateSessionProvider;
import com.smartacademic.entity.BorrowingRecord;
import com.smartacademic.enums.BorrowingStatus;
import com.smartacademic.repository.BorrowingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BorrowingRepositoryImpl implements BorrowingRepository {

    @Autowired
    private HibernateSessionProvider sessionFactory;

    @Override
    public BorrowingRecord save(BorrowingRecord record) {
        sessionFactory.getCurrentSession().persist(record);
        return record;
    }

    @Override
    public Optional<BorrowingRecord> findById(Long id) {
        BorrowingRecord record = sessionFactory.getCurrentSession()
                .get(BorrowingRecord.class, id);
        return Optional.ofNullable(record);
    }

    @Override
    public Optional<BorrowingRecord> findBySessionId(Long sessionId) {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT br FROM BorrowingRecord br " +
                                "LEFT JOIN FETCH br.details d " +
                                "LEFT JOIN FETCH d.equipment " +
                                "WHERE br.session.id = :sessionId", BorrowingRecord.class)
                .setParameter("sessionId", sessionId)
                .uniqueResultOptional();
    }

    @Override
    public List<BorrowingRecord> findPendingDispatch() {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT DISTINCT br FROM BorrowingRecord br " +
                                "LEFT JOIN FETCH br.details d " +
                                "LEFT JOIN FETCH d.equipment " +
                                "LEFT JOIN FETCH br.student s " +
                                "LEFT JOIN FETCH s.profile " +
                                "WHERE br.status = :status " +
                                "ORDER BY br.createdAt ASC", BorrowingRecord.class)
                .setParameter("status", BorrowingStatus.PENDING_DISPATCH)
                .list();
    }

    @Override
    public List<BorrowingRecord> findByStudentId(Long studentId) {
        return sessionFactory.getCurrentSession()
                .createQuery(
                        "SELECT DISTINCT br FROM BorrowingRecord br " +
                                "LEFT JOIN FETCH br.details d " +
                                "LEFT JOIN FETCH d.equipment " +
                                "WHERE br.student.id = :studentId " +
                                "ORDER BY br.createdAt DESC", BorrowingRecord.class)
                .setParameter("studentId", studentId)
                .list();
    }

    @Override
    public void update(BorrowingRecord record) {
        sessionFactory.getCurrentSession().merge(record);
    }
}