package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.entidades.Entidad;
import com.upc.huellasdeauxilio.entidades.ActualizarReporte;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.upc.huellasdeauxilio.entidades.Notificacion;
import com.upc.huellasdeauxilio.entidades.Reporte;
import com.upc.huellasdeauxilio.entidades.Ubicacion;
import com.upc.huellasdeauxilio.repositorios.ReporteRepositorio;
import com.upc.huellasdeauxilio.repositorios.UbicacionRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteServicio {

    @Autowired
    private ActualizarReporteServicio actualizarReporteServicio;

    @Autowired
    private ReporteRepositorio reporteRepositorio;

    @Autowired
    private NotificacionServicio notificacionServicio;

    @Autowired
    private UbicacionRepositorio ubicacionRepositorio;

    @Autowired
    private EntidadServicio entidadServicio;

    public Reporte insertar(Reporte reporte) {

        Ubicacion ubicacion = ubicacionRepositorio.findById(reporte.getUbicacion().getIdUbicacion()).orElse(null);

        if (ubicacion != null && ubicacion.getLatitud() != null && ubicacion.getLongitud() != null) {
            Entidad entidadCercana = entidadServicio.obtenerEntidadMasCercana(ubicacion.getLatitud(), ubicacion.getLongitud());

            reporte.setEntidad(entidadCercana);
        }

        Reporte reporteGuardado = reporteRepositorio.save(reporte);


        Notificacion notiCiudadano = new Notificacion();
        notiCiudadano.setReporte(reporteGuardado);

        notiCiudadano.setUsuario(reporteGuardado.getCiudadano().getUsuario());
        notiCiudadano.setTitulo("Reporte Registrado");
        notiCiudadano.setDescripcion("Tu reporte ha sido registrado y enviado exitosamente.");
        notificacionServicio.insertar(notiCiudadano);

        if(reporteGuardado.getEntidad() != null) {
            Notificacion notiEntidad = new Notificacion();
            notiEntidad.setReporte(reporteGuardado);

            notiEntidad.setUsuario(reporteGuardado.getEntidad().getUsuario());
            notiEntidad.setTitulo("Nuevo Reporte Asignado");
            notiEntidad.setDescripcion("Se te ha asignado un nuevo caso de maltrato animal.");
            notificacionServicio.insertar(notiEntidad);
        }

        return reporteGuardado;
    }

    @Transactional
    public Reporte actualizarEstado(Long idReporte, String nuevoEstado, String observacion) {
        if (!"ER".equals(nuevoEstado) && !"AT".equals(nuevoEstado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado solicitado inválido.");
        }
        String nota = observacion == null ? "" : observacion.trim();
        // La descripción del historial utiliza una columna VARCHAR(255).
        if (nota.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La observación admite hasta 150 caracteres.");
        }

        Reporte reporte = reporteRepositorio.buscarParaActualizar(idReporte)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reporte no encontrado."));
        actualizarReporteServicio.verificarEntidad(reporte);
        String estadoActual = reporte.getEstado();
        if (!("R".equals(estadoActual) && "ER".equals(nuevoEstado))
                && !("ER".equals(estadoActual) && "AT".equals(nuevoEstado))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La transición no está permitida para el estado actual.");
        }

        reporte.setEstado(nuevoEstado);
        Reporte reporteActualizado = reporteRepositorio.save(reporte);
        ActualizarReporte historial = new ActualizarReporte();
        historial.setReporte(reporteActualizado);
        historial.setDescripcionActu("Estado: " + estadoActual + " → " + nuevoEstado
                + (nota.isEmpty() ? "" : ". Observación: " + nota));
        actualizarReporteServicio.insertar(historial);

        Notificacion notificacion = new Notificacion();
        notificacion.setReporte(reporteActualizado);
        notificacion.setUsuario(reporteActualizado.getCiudadano().getUsuario());
        notificacion.setTitulo("Actualización de Reporte");
        notificacion.setFechaNotificacion(historial.getFechaActualizacion());
        notificacion.setDescripcion("Tu reporte ha cambiado al estado: " + nuevoEstado);
        notificacionServicio.insertar(notificacion);
        return reporteActualizado;
    }

    // para Ciudadano
    public List<Reporte> listarPorCiudadano(Long idCiudadano) {
        return reporteRepositorio.findByCiudadano_IdCiudadano(idCiudadano);
    }

    public Reporte buscarPorCodigoYCiudadano(Long idReporte, Long idCiudadano) {
        return reporteRepositorio.findByIdReporteAndCiudadano_IdCiudadano(idReporte, idCiudadano);
    }

    public Map<String, Long> obtenerResumenCiudadano(Long idCiudadano) {
        long total = reporteRepositorio.countByCiudadano_IdCiudadano(idCiudadano);
        long enRevision = reporteRepositorio.countByCiudadano_IdCiudadanoAndEstado(idCiudadano, "ER");
        long atendidos = reporteRepositorio.countByCiudadano_IdCiudadanoAndEstado(idCiudadano, "AT");

        Map<String, Long> resumen = new HashMap<>();
        resumen.put("total", total);
        resumen.put("enRevision", enRevision);
        resumen.put("atendidos", atendidos);
        return resumen;
    }

    public List<Reporte> filtrarReportes(Long idCiudadano, String estado, String urgencia, String distrito) {
        return reporteRepositorio.filtrarReportes(idCiudadano, estado, urgencia, distrito);
    }

    // para  Entidad
    public List<Reporte> listarPorEntidad(Long idEntidad) {
        return reporteRepositorio.findByEntidad_IdEntidadOrderByFechaReporteDesc(idEntidad);
    }

    public Map<String, Long> obtenerResumenEntidad(Long idEntidad) {
        long total = reporteRepositorio.countByEntidad_IdEntidad(idEntidad);
        long enRevision = reporteRepositorio.countByEntidad_IdEntidadAndEstado(idEntidad, "ER");
        long atendidos = reporteRepositorio.countByEntidad_IdEntidadAndEstado(idEntidad, "AT");

        Map<String, Long> resumen = new HashMap<>();
        resumen.put("total", total);
        resumen.put("enRevision", enRevision);
        resumen.put("atendidos", atendidos);
        return resumen;
    }

    public List<Reporte> filtrarReportesEntidad(Long idEntidad, String estado, String urgencia, String distrito) {
        return reporteRepositorio.filtrarReportesPorEntidad(idEntidad, estado, urgencia, distrito);
    }

    //Buscar Por Id cuando una entidad requiera de visualizar una entidads
    public Reporte buscarPorId(Long Id){

        return reporteRepositorio.findById(Id).orElse(null);
    }
}
