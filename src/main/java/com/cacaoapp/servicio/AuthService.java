package com.cacaoapp.servicio;

import com.cacaoapp.dao.UsuarioDao;
import com.cacaoapp.modelo.Usuario;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Locale;
import java.util.regex.Pattern;

public class AuthService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final UsuarioDao usuarioDao;

    public AuthService() {
        this(new UsuarioDao());
    }

    AuthService(UsuarioDao usuarioDao) {
        this.usuarioDao = usuarioDao;
    }

    public UsuarioPublico registrar(
            String nombre,
            String email,
            String telefono,
            String municipio,
            String password
    ) {
        String nombreValidado = required(nombre, "El nombre");
        String emailNormalizado = required(email, "El correo").toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(emailNormalizado).matches() || emailNormalizado.length() > 265) {
            throw new IllegalArgumentException("El correo electrónico no es válido.");
        }
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("La contraseña debe tener entre 8 y 72 caracteres.");
        }
        if (usuarioDao.findByEmail(emailNormalizado).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese correo.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(nombreValidado);
        usuario.setEmail(emailNormalizado);
        usuario.setTelefono(telefono);
        usuario.setMunicipio(municipio);
        usuario.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(12)));
        return UsuarioPublico.from(usuarioDao.create(usuario));
    }

    public UsuarioPublico iniciarSesion(String email, String password) {
        String emailNormalizado = required(email, "El correo").toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioDao.findByEmail(emailNormalizado).orElse(null);
        if (usuario == null || password == null
                || !BCrypt.checkpw(password, usuario.getPasswordHash())) {
            throw new IllegalArgumentException("Correo o contraseña incorrectos.");
        }
        return UsuarioPublico.from(usuario);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " es obligatorio.");
        }
        return value.trim();
    }

    public record UsuarioPublico(Long id, String nombre, String email) {
        private static UsuarioPublico from(Usuario usuario) {
            return new UsuarioPublico(usuario.getId(), usuario.getNombre(), usuario.getEmail());
        }
    }
}
