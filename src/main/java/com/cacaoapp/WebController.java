package com.cacaoapp;

import com.cacaoapp.dao.UsuarioDao;
import com.cacaoapp.modelo.Finca;
import com.cacaoapp.modelo.Productor;
import com.cacaoapp.modelo.RegistroCalidad;
import com.cacaoapp.modelo.Ruta;
import com.cacaoapp.modelo.Usuario;
import com.cacaoapp.servicio.AuthService;
import com.cacaoapp.util.HibernateUtil;
import jakarta.servlet.http.HttpSession;
import org.hibernate.Session;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Controller
public class WebController {
    private final AuthService authService = new AuthService();
    private final UsuarioDao usuarioDao = new UsuarioDao();

    @GetMapping("/")
    public String root(HttpSession session) {
        return redirectToHome(session);
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        return session.getAttribute("userId") == null ? "login" : "redirect:/inicio";
    }

    @PostMapping("/login")
    public String authenticate(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        try {
            AuthService.UsuarioPublico user = authService.iniciarSesion(email, password);
            session.setAttribute("userId", user.id());
            session.setAttribute("userName", user.nombre());
            return "redirect:/inicio";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/login";
        }
    }

    @GetMapping("/registro")
    public String register(HttpSession session) {
        return session.getAttribute("userId") == null ? "registro" : "redirect:/inicio";
    }

    @PostMapping("/registro")
    public String createAccount(
            @RequestParam String nombre,
            @RequestParam String email,
            @RequestParam(defaultValue = "") String telefono,
            @RequestParam(defaultValue = "") String municipio,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirect
    ) {
        if (!password.equals(confirmPassword)) {
            redirect.addFlashAttribute("error", "Las contraseñas no coinciden.");
            return "redirect:/registro";
        }
        try {
            AuthService.UsuarioPublico registered =
                    authService.registrar(nombre, email, telefono, municipio, password);
            Usuario usuario = usuarioDao.findById(registered.id()).orElseThrow();
            ensureProductor(usuario);
            redirect.addFlashAttribute("success", "Cuenta creada. Ya puedes iniciar sesión.");
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/registro";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/inicio")
    public String dashboard(HttpSession session, Model model) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        addCommon(model, session, "inicio");
        model.addAttribute("fincasCount", read(db -> db.createQuery(
                "select count(f.id) from Finca f where f.usuario.id = :userId", Long.class)
                .setParameter("userId", userId).getSingleResult()));
        model.addAttribute("rutasCount", read(db -> db.createQuery(
                "select count(r.id) from Ruta r where r.usuario.id = :userId", Long.class)
                .setParameter("userId", userId).getSingleResult()));
        model.addAttribute("calidadCount", read(db -> db.createQuery(
                "select count(q.id) from RegistroCalidad q where q.usuario.id = :userId", Long.class)
                .setParameter("userId", userId).getSingleResult()));
        return "inicio";
    }

    @GetMapping("/fincas")
    public String fincas(HttpSession session, Model model) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        addCommon(model, session, "fincas");
        model.addAttribute("fincas", read(db -> db.createQuery(
                "select f from Finca f join fetch f.productor where f.usuario.id = :userId order by f.id desc",
                Finca.class).setParameter("userId", userId).getResultList()));
        return "fincas";
    }

    @PostMapping("/fincas")
    public String createFinca(
            @RequestParam String nombre,
            @RequestParam double latitud,
            @RequestParam double longitud,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        if (nombre.isBlank() || latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180) {
            redirect.addFlashAttribute("error", "Revisa el nombre y las coordenadas de la finca.");
            return "redirect:/fincas";
        }
        HibernateUtil.inTransaction(db -> {
            Usuario usuario = db.find(Usuario.class, userId);
            Productor productor = ensureProductor(db, usuario);
            Finca finca = new Finca();
            finca.setUsuario(usuario);
            finca.setProductor(productor);
            finca.setNombre(nombre.trim());
            finca.setLatitud(latitud);
            finca.setLongitud(longitud);
            db.persist(finca);
            return null;
        });
        redirect.addFlashAttribute("success", "Finca guardada correctamente.");
        return "redirect:/fincas";
    }

    @PostMapping("/fincas/{id}/eliminar")
    public String deleteFinca(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        boolean deleted = HibernateUtil.inTransaction(db -> {
            Finca finca = db.createQuery(
                    "select f from Finca f where f.id = :id and f.usuario.id = :userId", Finca.class)
                    .setParameter("id", id).setParameter("userId", userId).uniqueResult();
            if (finca == null) return false;
            db.createQuery("delete from Ruta r where r.finca.id = :id and r.usuario.id = :userId")
                    .setParameter("id", id).setParameter("userId", userId).executeUpdate();
            db.createQuery("delete from RegistroCalidad q where q.finca.id = :id and q.usuario.id = :userId")
                    .setParameter("id", id).setParameter("userId", userId).executeUpdate();
            db.remove(finca);
            return true;
        });
        redirect.addFlashAttribute(deleted ? "success" : "error",
                deleted ? "Finca eliminada." : "No se encontró la finca.");
        return "redirect:/fincas";
    }

    @GetMapping("/mapa")
    public String map(HttpSession session, Model model) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        addCommon(model, session, "mapa");
        model.addAttribute("fincas", read(db -> db.createQuery(
                "from Finca f where f.usuario.id = :userId order by f.id desc", Finca.class)
                .setParameter("userId", userId).getResultList()));
        return "mapa";
    }

    @GetMapping("/rutas")
    public String routes(HttpSession session, Model model) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        addCommon(model, session, "rutas");
        model.addAttribute("fincas", read(db -> db.createQuery(
                "from Finca f where f.usuario.id = :userId order by f.nombre", Finca.class)
                .setParameter("userId", userId).getResultList()));
        model.addAttribute("rutas", read(db -> db.createQuery(
                "select r from Ruta r join fetch r.finca where r.usuario.id = :userId order by r.id desc",
                Ruta.class).setParameter("userId", userId).getResultList()));
        return "rutas";
    }

    @PostMapping("/rutas")
    public String createRoute(
            @RequestParam String nombre,
            @RequestParam Long fincaId,
            @RequestParam(defaultValue = "0") int puntos,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        if (nombre.isBlank() || puntos < 0) {
            redirect.addFlashAttribute("error", "Revisa el nombre y los puntos GPS de la ruta.");
            return "redirect:/rutas";
        }
        boolean created = HibernateUtil.inTransaction(db -> {
            Finca finca = db.createQuery(
                    "select f from Finca f where f.id = :id and f.usuario.id = :userId", Finca.class)
                    .setParameter("id", fincaId).setParameter("userId", userId).uniqueResult();
            if (finca == null) return false;
            Ruta ruta = new Ruta();
            ruta.setUsuario(db.find(Usuario.class, userId));
            ruta.setFinca(finca);
            ruta.setNombre(nombre.trim());
            ruta.setPuntos(Integer.toString(puntos));
            ruta.setGuardada(true);
            db.persist(ruta);
            return true;
        });
        redirect.addFlashAttribute(created ? "success" : "error",
                created ? "Ruta guardada correctamente." : "Selecciona una finca válida.");
        return "redirect:/rutas";
    }

    @PostMapping("/rutas/{id}/eliminar")
    public String deleteRoute(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        boolean deleted = HibernateUtil.inTransaction(db -> {
            Ruta ruta = db.createQuery(
                    "select r from Ruta r where r.id = :id and r.usuario.id = :userId", Ruta.class)
                    .setParameter("id", id).setParameter("userId", userId).uniqueResult();
            if (ruta == null) return false;
            db.remove(ruta);
            return true;
        });
        redirect.addFlashAttribute(deleted ? "success" : "error",
                deleted ? "Ruta eliminada." : "No se encontró la ruta.");
        return "redirect:/rutas";
    }

    @GetMapping("/calidad")
    public String quality(HttpSession session, Model model) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        addCommon(model, session, "calidad");
        model.addAttribute("fincas", read(db -> db.createQuery(
                "from Finca f where f.usuario.id = :userId order by f.nombre", Finca.class)
                .setParameter("userId", userId).getResultList()));
        model.addAttribute("registros", read(db -> db.createQuery(
                "select q from RegistroCalidad q join fetch q.finca where q.usuario.id = :userId order by q.id desc",
                RegistroCalidad.class).setParameter("userId", userId).getResultList()));
        return "calidad";
    }

    @PostMapping("/calidad")
    public String createQuality(
            @RequestParam Long fincaId,
            @RequestParam String fecha,
            @RequestParam double humedad,
            @RequestParam int fermentacion,
            @RequestParam double temperatura,
            @RequestParam(defaultValue = "") String observaciones,
            @RequestParam String estado,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        if (humedad < 0 || humedad > 100 || fermentacion < 0
                || temperatura < -50 || temperatura > 150
                || !List.of("bien_fermentado", "parcial", "sin_fermentar").contains(estado)) {
            redirect.addFlashAttribute("error", "Revisa los valores de calidad ingresados.");
            return "redirect:/calidad";
        }
        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(fecha);
        } catch (RuntimeException exception) {
            redirect.addFlashAttribute("error", "La fecha seleccionada no es válida.");
            return "redirect:/calidad";
        }
        boolean created = HibernateUtil.inTransaction(db -> {
            Finca finca = db.createQuery(
                    "select f from Finca f where f.id = :id and f.usuario.id = :userId", Finca.class)
                    .setParameter("id", fincaId).setParameter("userId", userId).uniqueResult();
            if (finca == null) return false;
            RegistroCalidad registro = new RegistroCalidad();
            registro.setUsuario(db.find(Usuario.class, userId));
            registro.setFinca(finca);
            registro.setFecha(Date.from(parsedDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            registro.setHumedad(humedad);
            registro.setFermentacion(fermentacion);
            registro.setTemperatura(temperatura);
            registro.setObservaciones(observaciones.trim());
            registro.setEstado(estado);
            db.persist(registro);
            return true;
        });
        redirect.addFlashAttribute(created ? "success" : "error",
                created ? "Registro de calidad guardado." : "Selecciona una finca válida.");
        return "redirect:/calidad";
    }

    @PostMapping("/calidad/{id}/eliminar")
    public String deleteQuality(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        Long userId = sessionUserId(session);
        if (userId == null) return "redirect:/login";
        boolean deleted = HibernateUtil.inTransaction(db -> {
            RegistroCalidad registro = db.createQuery(
                    "select q from RegistroCalidad q where q.id = :id and q.usuario.id = :userId",
                    RegistroCalidad.class).setParameter("id", id).setParameter("userId", userId)
                    .uniqueResult();
            if (registro == null) return false;
            db.remove(registro);
            return true;
        });
        redirect.addFlashAttribute(deleted ? "success" : "error",
                deleted ? "Registro eliminado." : "No se encontró el registro.");
        return "redirect:/calidad";
    }

    private String redirectToHome(HttpSession session) {
        return session.getAttribute("userId") == null ? "redirect:/login" : "redirect:/inicio";
    }

    private Long sessionUserId(HttpSession session) {
        Object value = session.getAttribute("userId");
        return value instanceof Long id ? id : null;
    }

    private void addCommon(Model model, HttpSession session, String activePage) {
        model.addAttribute("userName", session.getAttribute("userName"));
        model.addAttribute("activePage", activePage);
    }

    private <T> T read(java.util.function.Function<Session, T> query) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return query.apply(session);
        }
    }

    private void ensureProductor(Usuario usuario) {
        HibernateUtil.inTransaction(session -> {
            ensureProductor(session, usuario);
            return null;
        });
    }

    private Productor ensureProductor(Session session, Usuario usuario) {
        Productor productor = session.createQuery(
                "from Productor p where p.usuario.id = :userId order by p.id", Productor.class)
                .setParameter("userId", usuario.getId()).setMaxResults(1).uniqueResult();
        if (productor != null) return productor;
        Productor newProductor = new Productor();
        newProductor.setUsuario(usuario);
        newProductor.setNombre(usuario.getNombre());
        newProductor.setDocumento("WEB-" + usuario.getId());
        newProductor.setDireccion(usuario.getMunicipio() == null ? "Sin dirección" : usuario.getMunicipio());
        newProductor.setTelefono(usuario.getTelefono());
        session.persist(newProductor);
        return newProductor;
    }
}
