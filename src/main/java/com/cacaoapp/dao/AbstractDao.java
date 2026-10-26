package com.cacaoapp.dao;

import com.cacaoapp.util.HibernateUtil;

import java.util.List;
import java.util.Optional;

public abstract class AbstractDao<T> {
    private final Class<T> entityType;

    protected AbstractDao(Class<T> entityType) {
        this.entityType = entityType;
    }

    public Optional<T> findById(Long id) {
        try (var session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.ofNullable(session.find(entityType, id));
        }
    }

    public List<T> findAll() {
        try (var session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("from " + entityType.getSimpleName(), entityType)
                    .getResultList();
        }
    }

    public T create(T entity) {
        return HibernateUtil.inTransaction(session -> {
            session.persist(entity);
            return entity;
        });
    }

    public T update(T entity) {
        return HibernateUtil.inTransaction(session -> session.merge(entity));
    }

    public boolean deleteById(Long id) {
        return HibernateUtil.inTransaction(session -> {
            T entity = session.find(entityType, id);
            if (entity == null) {
                return false;
            }
            session.remove(entity);
            return true;
        });
    }
}
