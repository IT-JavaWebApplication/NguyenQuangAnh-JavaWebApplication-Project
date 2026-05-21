package com.smartacademic.repository.impl;

import com.smartacademic.entity.Equipment;
import com.smartacademic.repository.EquipmentRepository;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class EquipmentRepositoryImpl implements EquipmentRepository {

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public Equipment save(Equipment equipment) {
        sessionFactory.getCurrentSession().persist(equipment);
        return equipment;
    }

    @Override
    public Optional<Equipment> findById(Long id) {
        Equipment eq = sessionFactory.getCurrentSession().get(Equipment.class, id);
        return Optional.ofNullable(eq);
    }

    @Override
    public Optional<Equipment> findByCode(String code) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Equipment e WHERE e.code = :code", Equipment.class)
                .setParameter("code", code)
                .uniqueResultOptional();
    }

    @Override
    public List<Equipment> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Equipment e ORDER BY e.name ASC", Equipment.class)
                .list();
    }

    @Override
    public List<Equipment> findAllActive() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Equipment e WHERE e.isActive = true AND e.available > 0 ORDER BY e.name ASC", Equipment.class)
                .list();
    }

    @Override
    public void update(Equipment equipment) {
        sessionFactory.getCurrentSession().merge(equipment);
    }

    @Override
    public void delete(Long id) {
        // Soft delete
        Equipment equipment = sessionFactory.getCurrentSession().get(Equipment.class, id);
        if (equipment != null) {
            equipment.setIsActive(false);
            sessionFactory.getCurrentSession().merge(equipment);
        }
    }

    @Override
    public boolean existsByCode(String code) {
        Long count = sessionFactory.getCurrentSession()
                .createQuery("SELECT COUNT(e) FROM Equipment e WHERE e.code = :code", Long.class)
                .setParameter("code", code)
                .uniqueResult();
        return count != null && count > 0;
    }
}