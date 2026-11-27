package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.entidades.ActualizarReporte;
import com.upc.huellasdeauxilio.entidades.Reporte;
import com.upc.huellasdeauxilio.entidades.Entidad;
import com.upc.huellasdeauxilio.repositorios.ActualizarReporteRepositorio;
import com.upc.huellasdeauxilio.repositorios.ReporteRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActualizarReporteServicio {

    @Autowired
    private ActualizarReporteRepositorio actualizarReporteRepositorio;

    @Autowired
    private ReporteRepositorio reporteRepositorio;

    public void verificarEntidad(Reporte reporte) {
        verificarEntidad(reporte.getEntidad());
    }

    public void verificarEntidad(Entidad entidad) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida.");
        }
        boolean esEntidad = autenticacion.getAuthorities().stream()
                .anyMatch(rol -> "ROLE_ENTIDAD".equals(rol.getAuthority()));
        if (!esEntidad || entidad == null || entidad.getUsuario() == null
                || !autenticacion.getName().equals(entidad.getUsuario().getCorreo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El reporte no pertenece a tu entidad.");
        }
    }

    @Transactional
    public ActualizarReporte insertar(ActualizarReporte actualizacion) {
        if (actualizacion.getIdActualizacion() != null || actualizacion.getReporte() == null
                || actualizacion.getReporte().getIdReporte() == null
                || actualizacion.getDescripcionActu() == null || actualizacion.getDescripcionActu().isBlank()
                || actualizacion.getDescripcionActu().length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos del historial inválidos.");
        }
        Reporte reporte = reporteRepositorio.buscarParaActualizar(actualizacion.getReporte().getIdReporte())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado."));
        verificarEntidad(reporte);
        actualizacion.setReporte(reporte);
        actualizacion.setFechaActualizacion(LocalDateTime.now());
        return actualizarReporteRepositorio.save(actualizacion);
    }

    @Transactional(readOnly = true)
    public List<ActualizarReporte> listarPorReporte(Long idReporte) {
        Reporte reporte = reporteRepositorio.findById(idReporte)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado."));
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida.");
        }
        boolean esCiudadano = autenticacion.getAuthorities().stream()
                .anyMatch(rol -> "ROLE_CIUDADANO".equals(rol.getAuthority()));
        if (esCiudadano) {
            if (reporte.getCiudadano() == null || reporte.getCiudadano().getUsuario() == null
                    || !autenticacion.getName().equals(reporte.getCiudadano().getUsuario().getCorreo())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El reporte no pertenece a tu cuenta.");
            }
        } else {
            verificarEntidad(reporte);
        }
        return actualizarReporteRepositorio.findByReporte_IdReporteOrderByFechaActualizacionAsc(idReporte);
    }
}
