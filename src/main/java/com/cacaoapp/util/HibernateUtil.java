package com.cacaoapp.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import java.util.function.Function;

public final class HibernateUtil {
    private static volatile SessionFactory sessionFactory;

    private HibernateUtil() {}

    public static SessionFactory getSessionFactory() {
        SessionFactory factory = sessionFactory;
        if (factory == null) {
            synchronized (HibernateUtil.class) {
                factory = sessionFactory;
                if (factory == null) {
                    factory = buildSessionFactory();
                    sessionFactory = factory;
                }
            }
        }
        return factory;
    }

    public static <T> T inTransaction(Function<Session, T> operation) {
        try (Session session = getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                T result = operation.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw exception;
            }
        }
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null) {
            if (sessionFactory.isOpen()) {
                sessionFactory.close();
            }
            sessionFactory = null;
        }
    }

    private static SessionFactory buildSessionFactory() {
        Configuration configuration = new Configuration().configure();
        String jdbcUrl = environmentValue("HIBERNATE_JDBC_URL");
        if (jdbcUrl == null) {
            jdbcUrl = "jdbc:h2:mem:cacaoapp;DB_CLOSE_DELAY=-1;MODE=MySQL";
        }
        configuration.setProperty("connection.url", jdbcUrl);

        if (jdbcUrl.startsWith("jdbc:mysql:")) {
            configuration.setProperty("connection.driver_class", "com.mysql.cj.jdbc.Driver");
            configuration.setProperty("dialect", "org.hibernate.dialect.MySQLDialect");
        } else if (jdbcUrl.startsWith("jdbc:h2:")) {
            configuration.setProperty("connection.driver_class", "org.h2.Driver");
            configuration.setProperty("dialect", "org.hibernate.dialect.H2Dialect");
        } else {
            throw new IllegalArgumentException(
                    "HIBERNATE_JDBC_URL debe usar una URL jdbc:mysql: o jdbc:h2:.");
        }

        String username = environmentValue("HIBERNATE_DB_USER");
        String password = environmentValue("HIBERNATE_DB_PASSWORD");
        configuration.setProperty(
                "connection.username",
                username != null ? username : jdbcUrl.startsWith("jdbc:h2:") ? "sa" : "root");
        configuration.setProperty("connection.password", password != null ? password : "");
        return configuration.buildSessionFactory();
    }

    private static String environmentValue(String variable) {
        String value = System.getenv(variable);
        return value == null || value.isBlank() ? null : value;
    }
}
