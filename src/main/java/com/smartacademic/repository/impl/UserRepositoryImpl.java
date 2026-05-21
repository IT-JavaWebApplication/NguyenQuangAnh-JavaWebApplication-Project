package com.smartacademic.repository.impl;

import com.smartacademic.entity.User;
import com.smartacademic.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public User save(User user) {
        sessionFactory.getCurrentSession().persist(user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        User user = sessionFactory.getCurrentSession().get(User.class, id);
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String hql = "FROM User u WHERE u.username = :username";
        return entityManager.createQuery(hql, User.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM User u WHERE u.email = :email AND u.isActive = true", User.class)
                .setParameter("email", email)
                .uniqueResultOptional();
    }

    @Override
    public boolean existsByUsername(String username) {
        Long count = sessionFactory.getCurrentSession()
                .createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = sessionFactory.getCurrentSession()
                .createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public List<User> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM User u WHERE u.isActive = true ORDER BY u.createdAt DESC", User.class)
                .list();
    }

    @Override
    public void update(User user) {
        sessionFactory.getCurrentSession().merge(user);
    }
}