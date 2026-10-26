package com.cacaoapp.dao;

import com.cacaoapp.modelo.Usuario;
import com.cacaoapp.util.HibernateUtil;

import java.util.Optional;

public class UsuarioDao extends AbstractDao<Usuario> {
    public UsuarioDao() {
        super(Usuario.class);
    }

    public Optional<Usuario> findByEmail(String email) {
        try (var session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                            "from Usuario u where lower(u.email) = :email", Usuario.class)
                    .setParameter("email", email.toLowerCase())
                    .uniqueResultOptional();
        }
    }
}
