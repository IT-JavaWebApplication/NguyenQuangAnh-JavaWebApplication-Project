package com.smartacademic.repository.impl;

import com.smartacademic.config.HibernateSessionProvider;
import com.smartacademic.entity.User;
import com.smartacademic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    @Autowired
    private HibernateSessionProvider session;

    @Override
    public User save(User user) {
        session.getCurrentSession().persist(user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(session.getCurrentSession().get(User.class, id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return session.getCurrentSession()
                .createQuery("FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return session.getCurrentSession()
                .createQuery("FROM User u WHERE u.email = :email AND u.isActive = true", User.class)
                .setParameter("email", email)
                .uniqueResultOptional();
    }

    @Override
    public boolean existsByUsername(String username) {
        Long count = session.getCurrentSession()
                .createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = session.getCurrentSession()
                .createQuery("SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .uniqueResult();
        return count != null && count > 0;
    }

    @Override
    public java.util.List<User> findAll() {
        return session.getCurrentSession()
                .createQuery("FROM User u WHERE u.isActive = true ORDER BY u.createdAt DESC", User.class)
                .list();
    }

    @Override
    public void update(User user) {
        session.getCurrentSession().merge(user);
    }
}
