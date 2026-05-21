package com.smartacademic.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Adapter trả về Hibernate Session từ EntityManager (Spring-managed,
 * thread-bound) để Service/Repository vẫn dùng được Hibernate native API.
 */
@Component
public class HibernateSessionProvider {

    @PersistenceContext
    private EntityManager entityManager;

    public Session getCurrentSession() {
        return entityManager.unwrap(Session.class);
    }
}
