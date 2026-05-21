package com.smartacademic.repository.impl;

import com.smartacademic.config.HibernateSessionProvider;
import com.smartacademic.entity.Payment;
import com.smartacademic.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    @Autowired
    private HibernateSessionProvider session;

    @Override
    public Payment save(Payment payment) {
        session.getCurrentSession().persist(payment);
        return payment;
    }

    @Override
    public void update(Payment payment) {
        session.getCurrentSession().merge(payment);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(session.getCurrentSession().get(Payment.class, id));
    }

    @Override
    public Optional<Payment> findByTxnRef(String txnRef) {
        return session.getCurrentSession()
                .createQuery("FROM Payment p WHERE p.txnRef = :ref", Payment.class)
                .setParameter("ref", txnRef)
                .uniqueResultOptional();
    }

    @Override
    public Optional<Payment> findLatestPendingBySessionId(Long sessionId) {
        return session.getCurrentSession()
                .createQuery(
                        "FROM Payment p " +
                                "WHERE p.session.id = :sid " +
                                "AND p.status IN ('PENDING','PROCESSING') " +
                                "ORDER BY p.createdAt DESC", Payment.class)
                .setParameter("sid", sessionId)
                .setMaxResults(1)
                .uniqueResultOptional();
    }

    @Override
    public List<Payment> findByPayerId(Long payerId) {
        return session.getCurrentSession()
                .createQuery(
                        "SELECT p FROM Payment p " +
                                "LEFT JOIN FETCH p.session " +
                                "WHERE p.payer.id = :uid " +
                                "ORDER BY p.createdAt DESC", Payment.class)
                .setParameter("uid", payerId)
                .list();
    }
}
