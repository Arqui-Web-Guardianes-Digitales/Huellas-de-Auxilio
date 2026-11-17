package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.entidades.Notificacion;
import com.upc.huellasdeauxilio.repositorios.NotificacionRepositorio;
import com.upc.huellasdeauxilio.entidades.Usuario;
import com.upc.huellasdeauxilio.repositorios.UsuarioRepositorio;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificacionServicio {

    @Autowired
    private NotificacionRepositorio notificacionRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    public Notificacion insertar(Notificacion notificacion) {

        notificacion.setEstadoNotificacion(false);

        return notificacionRepositorio.save(notificacion);
    }

    public List<Notificacion> listarPorUsuario(Long idUsuario) {
        comprobarUsuario(idUsuario);
        return notificacionRepositorio.findByUsuario_IdUsuarioOrderByFechaNotificacionDesc(idUsuario);
    }

    public Notificacion marcarComoLeida(Long idNotificacion) {
        Notificacion notificacion = notificacionRepositorio.findById(idNotificacion).orElse(null);
        if (notificacion != null) {
            comprobarUsuario(notificacion.getUsuario() == null ? null : notificacion.getUsuario().getIdUsuario());
            notificacion.setEstadoNotificacion(true); // true = leída
            return notificacionRepositorio.save(notificacion);
        }
        return null;
    }

    private void comprobarUsuario(Long idUsuario) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida.");
        }
        Usuario usuario = usuarioRepositorio.findByCorreo(autenticacion.getName());
        if (usuario == null || idUsuario == null || !idUsuario.equals(usuario.getIdUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes consultar o leer tus propias notificaciones.");
        }
    }

}
