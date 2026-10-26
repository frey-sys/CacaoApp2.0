package com.cacaoapp;

import com.cacaoapp.dao.FincaDao;
import com.cacaoapp.dao.ProductorDao;
import com.cacaoapp.dao.RegistroCalidadDao;
import com.cacaoapp.dao.RutaDao;
import com.cacaoapp.dao.UsuarioDao;
import com.cacaoapp.modelo.Finca;
import com.cacaoapp.modelo.Productor;
import com.cacaoapp.modelo.RegistroCalidad;
import com.cacaoapp.modelo.Ruta;
import com.cacaoapp.modelo.Usuario;
import com.cacaoapp.servicio.AuthService;
import com.cacaoapp.util.HibernateUtil;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        UsuarioDao usuarioDao = new UsuarioDao();
        ProductorDao productorDao = new ProductorDao();
        FincaDao fincaDao = new FincaDao();
        RutaDao rutaDao = new RutaDao();
        RegistroCalidadDao calidadDao = new RegistroCalidadDao();

        try {
            String uniqueId = UUID.randomUUID().toString().substring(0, 8);
            String email = "prueba-" + uniqueId + "@cacaoapp.local";
            String password = UUID.randomUUID().toString() + "A1!";
            AuthService authService = new AuthService();

            authService.registrar("Usuario de prueba", email, "3001234567", "Tumaco", password);
            Usuario usuario = usuarioDao.findByEmail(email).orElseThrow();
            System.out.printf("[CREATE] Usuario id=%d email=%s%n", usuario.getId(), usuario.getEmail());
            authService.iniciarSesion(email, password);

            Productor productor = new Productor();
            productor.setUsuario(usuario);
            productor.setNombre("Productor de prueba");
            productor.setDocumento("DOC-" + uniqueId);
            productor.setDireccion("Vereda El Cacao");
            productor.setTelefono("3001234567");
            productor = productorDao.create(productor);
            System.out.printf("[CREATE] Productor id=%d nombre=%s%n", productor.getId(), productor.getNombre());

            Finca finca = new Finca();
            finca.setUsuario(usuario);
            finca.setProductor(productor);
            finca.setNombre("Finca de prueba");
            finca.setLatitud(1.2136);
            finca.setLongitud(-77.2811);
            finca = fincaDao.create(finca);
            System.out.printf(Locale.ROOT, "[CREATE] Finca id=%d coordenadas=%.4f,%.4f%n",
                    finca.getId(), finca.getLatitud(), finca.getLongitud());

            Ruta ruta = new Ruta();
            ruta.setUsuario(usuario);
            ruta.setFinca(finca);
            ruta.setNombre("Ruta de prueba");
            ruta.setPuntos("[{\"lat\":1.2136,\"lng\":-77.2811}]");
            ruta.setGuardada(true);
            ruta = rutaDao.create(ruta);
            System.out.printf("[CREATE] Ruta id=%d nombre=%s%n", ruta.getId(), ruta.getNombre());

            RegistroCalidad calidad = new RegistroCalidad();
            calidad.setUsuario(usuario);
            calidad.setFinca(finca);
            calidad.setFecha(new Date());
            calidad.setHumedad(7.5);
            calidad.setFermentacion(6);
            calidad.setTemperatura(25.0);
            calidad.setObservaciones("Prueba de persistencia");
            calidad.setEstado("Bien fermentado");
            calidad = calidadDao.create(calidad);
            System.out.printf("[CREATE] RegistroCalidad id=%d estado=%s%n",
                    calidad.getId(), calidad.getEstado());

            usuario = usuarioDao.findById(usuario.getId()).orElseThrow();
            productor = productorDao.findById(productor.getId()).orElseThrow();
            finca = fincaDao.findById(finca.getId()).orElseThrow();
            ruta = rutaDao.findById(ruta.getId()).orElseThrow();
            calidad = calidadDao.findById(calidad.getId()).orElseThrow();
            System.out.printf("[READ] Usuario=%s | Productor=%s | Finca=%s | Ruta=%s | Calidad=%s%n",
                    usuario.getEmail(), productor.getNombre(), finca.getNombre(),
                    ruta.getNombre(), calidad.getEstado());

            usuario.setNombre("Usuario actualizado");
            usuarioDao.update(usuario);
            productor.setNombre("Productor actualizado");
            productorDao.update(productor);
            finca.setNombre("Finca actualizada");
            fincaDao.update(finca);
            ruta.setNombre("Ruta actualizada");
            rutaDao.update(ruta);
            calidad.setObservaciones("Registro actualizado");
            calidadDao.update(calidad);
            System.out.printf("[UPDATE] Usuario=%s | Productor=%s | Finca=%s | Ruta=%s | Calidad=%s%n",
                    usuario.getNombre(), productor.getNombre(), finca.getNombre(),
                    ruta.getNombre(), calidad.getObservaciones());

            System.out.printf("[DELETE] RegistroCalidad=%s%n", calidadDao.deleteById(calidad.getId()));
            System.out.printf("[DELETE] Ruta=%s%n", rutaDao.deleteById(ruta.getId()));
            System.out.printf("[DELETE] Finca=%s%n", fincaDao.deleteById(finca.getId()));
            System.out.printf("[DELETE] Productor=%s%n", productorDao.deleteById(productor.getId()));
            System.out.printf("[DELETE] Usuario=%s%n", usuarioDao.deleteById(usuario.getId()));
        } finally {
            HibernateUtil.shutdown();
        }
    }
}
