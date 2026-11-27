package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.dtos.SolicitudDTO;
import com.upc.huellasdeauxilio.entidades.Ciudadano;
import com.upc.huellasdeauxilio.entidades.Entidad;
import com.upc.huellasdeauxilio.repositorios.EntidadRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import com.upc.huellasdeauxilio.entidades.Mascota;
import com.upc.huellasdeauxilio.entidades.Notificacion;
import com.upc.huellasdeauxilio.entidades.Solicitud;
import com.upc.huellasdeauxilio.repositorios.CiudadanoRepositorio;
import com.upc.huellasdeauxilio.repositorios.MascotaRepositorio;
import com.upc.huellasdeauxilio.repositorios.SolicitudRepositorio;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class SolicitudServicio {
    @Autowired
    private SolicitudRepositorio solicitudRepositorio;

    @Autowired
    private CiudadanoRepositorio ciudadanoRepositorio;

    @Autowired
    private MascotaServicio mascotaServicio;

    @Autowired
    private NotificacionServicio notificacionServicio;

    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private MascotaRepositorio mascotaRepositorio;
    @Autowired
    private EntidadRepositorio entidadRepositorio;

    @Transactional
    public SolicitudDTO insertar(Long idCiudadano, SolicitudDTO solicitudDTO)
    {
        if (idCiudadano == null
                || solicitudDTO == null
                || solicitudDTO.getMascotaIdMascota() == null
                || solicitudDTO.getTipoVivienda() == null
                || solicitudDTO.getTipoVivienda().isBlank()
                || solicitudDTO.getMotivo() == null
                || solicitudDTO.getMotivo().isBlank()
                || solicitudDTO.getExperiencia() == null) {

            return null;
        }

        Ciudadano ciudadano = ciudadanoRepositorio
                .findById(idCiudadano)
                .orElse(null);

        if (ciudadano == null || ciudadano.getUsuario() == null) {
            return null;
        }

        Mascota mascota = mascotaRepositorio
                .findById(solicitudDTO.getMascotaIdMascota())
                .orElse(null);

        if (mascota == null
                || !Boolean.TRUE.equals(mascota.getEstado())) {

            return null;
        }

        // Convertir DTO a entidad
        Solicitud solicitud = modelMapper.map(
                solicitudDTO,
                Solicitud.class
        );

        // No aceptar estos valores desde el cliente
        solicitud.setIdSolicitud(null);
        solicitud.setCiudadano(ciudadano);
        solicitud.setMascota(mascota);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setEstadoSolicitud("EN_REVISION");

        solicitud.setTipoVivienda(
                solicitudDTO.getTipoVivienda().trim()
        );
        solicitud.setMotivo(solicitudDTO.getMotivo().trim());
        solicitud.setExperiencia(solicitudDTO.getExperiencia());

        Solicitud solicitudGuardada =
                solicitudRepositorio.save(solicitud);

        Notificacion notiCiudadano = new Notificacion();

        notiCiudadano.setSolicitud(solicitudGuardada);
        notiCiudadano.setUsuario(ciudadano.getUsuario());
        notiCiudadano.setTitulo("Solicitud enviada");
        notiCiudadano.setDescripcion(
                "Tu solicitud para adoptar a "
                        + mascota.getNombre()
                        + " fue enviada correctamente."
        );
        notiCiudadano.setFechaNotificacion(LocalDateTime.now());

        notificacionServicio.insertar(notiCiudadano);
        notificarEntidad(solicitudGuardada, "Nueva solicitud de adopción", "Se recibió una solicitud de adopción para " + mascota.getNombre() + ".");

        // Convertir la entidad guardada a DTO
        SolicitudDTO respuesta = modelMapper.map(
                solicitudGuardada,
                SolicitudDTO.class
        );

        respuesta.setMascotaIdMascota(mascota.getIdMascota());
        respuesta.setCiudadanoIdCiudadano(
                ciudadano.getIdCiudadano()
        );

        return respuesta;
    }

    public List<SolicitudDTO> listarPorCiudadano(Long idCiudadano)
    {

        if (idCiudadano == null) {
            return null;
        }

        Ciudadano ciudadano = ciudadanoRepositorio
                .findById(idCiudadano)
                .orElse(null);

        if (ciudadano == null) {
            return null;
        }

        return solicitudRepositorio
                .findByCiudadano_IdCiudadanoOrderByFechaSolicitudDesc(idCiudadano).stream()
                .map(solicitud ->
                {SolicitudDTO dto = modelMapper.map(solicitud, SolicitudDTO.class);
                    dto.setMascotaIdMascota(solicitud.getMascota().getIdMascota());
                    dto.setCiudadanoIdCiudadano(solicitud.getCiudadano().getIdCiudadano());
                    return dto;
                }).collect(Collectors.toList());
    }

    public SolicitudDTO buscarPorCodigoYCiudadano(Long idSolicitud, Long idCiudadano)
    {
        if (idSolicitud == null || idCiudadano == null)
        {
            return null;
        }

        Solicitud solicitud = solicitudRepositorio.findByIdSolicitudAndCiudadano_IdCiudadano(idSolicitud, idCiudadano);

        if (solicitud == null)
        {
            return null;
        }

        SolicitudDTO dto = modelMapper.map(solicitud, SolicitudDTO.class);
        dto.setMascotaIdMascota(solicitud.getMascota().getIdMascota());
        dto.setCiudadanoIdCiudadano(solicitud.getCiudadano().getIdCiudadano());
        return dto;
    }

    //esta es una funcion extra para quitar especios extra y transformar
    //mayusculas a minusculas y facilitar la filtracion
    private String normalizarFiltro(String valor) {
        if (valor == null || valor.isBlank()) {
            return "todos";
        }

        return valor.trim().toLowerCase(Locale.ROOT);
    }

    public List<SolicitudDTO> filtrarSolicitudes(Long idCiudadano, String nombreMascota, String estado)
    {
        Ciudadano ciudadano = buscarCiudadano(idCiudadano);

        if (ciudadano == null)
        {
            return null;
        }

        String estadoFiltro = normalizarFiltro(estado);

        if (!estadoFiltro.equals("todos")
                && !estadoFiltro.equals("en_revision")
                && !estadoFiltro.equals("aprobada")
                && !estadoFiltro.equals("rechazada")) {

            return null;
        }

        return solicitudRepositorio.filtrarSolicitudes(idCiudadano, normalizarFiltro(nombreMascota), estadoFiltro)
                .stream()
                .map(solicitud ->
                {
                    SolicitudDTO dto = modelMapper.map(solicitud, SolicitudDTO.class);
                    dto.setMascotaIdMascota(solicitud.getMascota().getIdMascota());
                    dto.setCiudadanoIdCiudadano(solicitud.getCiudadano().getIdCiudadano());
                    return dto;
                }).collect(Collectors.toList());

    }

    public List<SolicitudDTO> listarPorMascota(Long idMascota) {
        verificarMascota(idMascota);

        return solicitudRepositorio.findByMascota_IdMascotaOrderByFechaSolicitudDesc(idMascota).stream()
                .map(this::convertirParaEntidad).collect(Collectors.toList());
    }

    public SolicitudDTO buscarPorSolicitudYMascota(Long idSolicitud, Long idMascota) {
        verificarMascota(idMascota);

        if (idSolicitud == null || idSolicitud <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de solicitud inválido.");
        }

        Solicitud solicitud = solicitudRepositorio.findByIdSolicitudAndMascota_IdMascota(idSolicitud, idMascota);

        if (solicitud == null)
        {
            return null;
        }

        SolicitudDTO dto = convertirParaEntidad(solicitud);
        Ciudadano ciudadano = solicitud.getCiudadano();
        if (ciudadano != null) {
            dto.setDniSolicitante(ciudadano.getDni());
            if (ciudadano.getUsuario() != null) {
                dto.setCorreoSolicitante(ciudadano.getUsuario().getCorreo());
                dto.setTelefonoSolicitante(ciudadano.getUsuario().getTelefono());
            }
        }
        dto.setMascotaIdMascota(solicitud.getMascota().getIdMascota());
        dto.setCiudadanoIdCiudadano(solicitud.getCiudadano().getIdCiudadano());
        return dto;
    }

    public List<SolicitudDTO> listarPorEntidad(Long idEntidad) {
        verificarEntidad(idEntidad);

        if (idEntidad == null) {
            return null;
        }

        return solicitudRepositorio
                .findByMascota_Entidad_IdEntidadOrderByFechaSolicitudDesc(idEntidad)
                .stream()
                .map(this::convertirParaEntidad)
                .collect(Collectors.toList());
    }

    public List<SolicitudDTO> filtrarSolicitudesPorMascota(Long idMascota, String codigo, String estado) {
        verificarMascota(idMascota);

        String estadoFiltro = normalizarFiltro(estado);

        if (!estadoFiltro.equals("todos")
                && !estadoFiltro.equals("en_revision")
                && !estadoFiltro.equals("rechazada")
                && !estadoFiltro.equals("aprobada")) {
            return null;
        }

        return solicitudRepositorio.filtrarSolicitudesPorMascota(idMascota, normalizarFiltro(codigo), estadoFiltro).stream()
                .map(this::convertirParaEntidad).collect(Collectors.toList());
    }

    private Ciudadano buscarCiudadano(Long idCiudadano) {
        if (idCiudadano == null) {
            return null;
        }

        return ciudadanoRepositorio
                .findById(idCiudadano)
                .orElse(null);
    }

    public List<SolicitudDTO> filtrarSolicitudesPorEntidad(
            Long idEntidad,
            String codigo,
            String estado,
            String especie
    ) {
        verificarEntidad(idEntidad);
        if (idEntidad == null) {
            return null;
        }

        String codigoFiltro = normalizarFiltro(codigo);
        String estadoFiltro = normalizarFiltro(estado);
        String especieFiltro = normalizarFiltro(especie);

        if (!estadoFiltro.equals("todos")
                && !estadoFiltro.equals("en_revision")
                && !estadoFiltro.equals("aprobada")
                && !estadoFiltro.equals("rechazada")) {
            return null;
        }

        if (!especieFiltro.equals("todos")
                && !especieFiltro.equals("perro")
                && !especieFiltro.equals("gato")) {
            return null;
        }

        return solicitudRepositorio.filtrarSolicitudesPorEntidad(
                        idEntidad,
                        codigoFiltro,
                        estadoFiltro,
                        especieFiltro
                )
                .stream()
                .map(this::convertirParaEntidad)
                .collect(Collectors.toList());
    }

    @Transactional
    public SolicitudDTO rechazarSolicitud(Long idSolicitud) {

        Solicitud solicitud = buscarSolicitudGestionable(idSolicitud);


        solicitud.setEstadoSolicitud("RECHAZADA");

        Solicitud solicitudActualizada =
                solicitudRepositorio.save(solicitud);

        //notificar al ciudadano solicitante
        if (solicitud.getCiudadano() != null
                && solicitud.getCiudadano().getUsuario() != null) {

            Notificacion notificacion = new Notificacion();

            notificacion.setSolicitud(solicitudActualizada);
            notificacion.setUsuario(
                    solicitud.getCiudadano().getUsuario()
            );
            notificacion.setTitulo("Solicitud rechazada");
            notificacion.setDescripcion(
                    "Tu solicitud de adopción para "
                            + solicitud.getMascota().getNombre()
                            + " ha sido rechazada."
            );
            notificacion.setFechaNotificacion(LocalDateTime.now());

            notificacionServicio.insertar(notificacion);
        }

        notificarEntidad(solicitudActualizada, "Solicitud rechazada", "La solicitud " + solicitudActualizada.getIdSolicitud() + " fue rechazada.");
        return convertirADTO(solicitudActualizada);
    }

    @Transactional
    public SolicitudDTO aprobarSolicitud(Long idSolicitud) {

        Solicitud solicitud = buscarSolicitudGestionable(idSolicitud);


        Mascota mascota = solicitud.getMascota();

        if (mascota == null) {
            return null;
        }

        if (!Boolean.TRUE.equals(mascota.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La mascota ya no está disponible.");
        }

        //aprobar la solicitud seleccionada
        solicitud.setEstadoSolicitud("APROBADA");

        Solicitud solicitudAprobada =
                solicitudRepositorio.save(solicitud);

        //buscar las demás solicitudes que siguen en revisión
        List<Solicitud> solicitudesPendientes =
                solicitudRepositorio
                        .findByMascota_IdMascotaAndEstadoSolicitud(
                                mascota.getIdMascota(),
                                "EN_REVISION"
                        );

        long idSolicitudAprobada = solicitudAprobada.getIdSolicitud();
        long idUsuarioAprobado = 0;

        if (solicitudAprobada.getCiudadano() != null
                && solicitudAprobada.getCiudadano().getUsuario() != null) {
            idUsuarioAprobado = solicitudAprobada.getCiudadano().getUsuario().getIdUsuario();
        }

        for (Solicitud otraSolicitud : solicitudesPendientes) {

            if (otraSolicitud.getIdSolicitud() != idSolicitudAprobada) {
                otraSolicitud.setEstadoSolicitud("RECHAZADA");

                Solicitud solicitudRechazada =
                        solicitudRepositorio.save(otraSolicitud);

                if (otraSolicitud.getCiudadano() != null
                        && otraSolicitud.getCiudadano().getUsuario() != null) {

                    long idUsuarioRechazado = otraSolicitud.getCiudadano().getUsuario().getIdUsuario();

                    if (idUsuarioRechazado != idUsuarioAprobado) {
                        Notificacion notificacionRechazo = new Notificacion();
                        notificacionRechazo.setSolicitud(solicitudRechazada);
                        notificacionRechazo.setUsuario(otraSolicitud.getCiudadano().getUsuario());
                        notificacionRechazo.setTitulo("Solicitud rechazada");
                        notificacionRechazo.setDescripcion(
                                "Tu solicitud de adopción N.º "
                                        + solicitudRechazada.getIdSolicitud()
                                        + " para "
                                        + mascota.getNombre()
                                        + " fue rechazada porque se aprobó otra solicitud."
                        );
                        notificacionRechazo.setFechaNotificacion(LocalDateTime.now());

                        notificacionServicio.insertar(notificacionRechazo);
                    }
                }

                notificarEntidad(solicitudRechazada, "Solicitud rechazada", "La solicitud " + solicitudRechazada.getIdSolicitud() + " fue rechazada al aprobar otra solicitud para la misma mascota.");
            }
        }

        //la mascota deja de estar disponible para adopción
        mascotaServicio.cambiarEstado(
                mascota.getIdMascota(),
                false
        );

        //notificar al ciudadano cuya solicitud fue aprobada
        if (solicitud.getCiudadano() != null
                && solicitud.getCiudadano().getUsuario() != null) {

            Notificacion notificacion = new Notificacion();

            notificacion.setSolicitud(solicitudAprobada);
            notificacion.setUsuario(
                    solicitud.getCiudadano().getUsuario()
            );
            notificacion.setTitulo("Solicitud aprobada");
            notificacion.setDescripcion(
                    "Tu solicitud de adopción para "
                            + mascota.getNombre()
                            + " ha sido aprobada."
            );
            notificacion.setFechaNotificacion(LocalDateTime.now());

            notificacionServicio.insertar(notificacion);
        }

        notificarEntidad(solicitudAprobada, "Solicitud aprobada", "La solicitud " + solicitudAprobada.getIdSolicitud() + " fue aprobada.");
        return convertirADTO(solicitudAprobada);
    }

    private void notificarEntidad(Solicitud solicitud, String titulo, String descripcion) {
        if (solicitud.getMascota() != null && solicitud.getMascota().getEntidad() != null
                && solicitud.getMascota().getEntidad().getUsuario() != null) {
            Notificacion notificacion = new Notificacion();
            notificacion.setSolicitud(solicitud);
            notificacion.setUsuario(solicitud.getMascota().getEntidad().getUsuario());
            notificacion.setTitulo(titulo);
            notificacion.setDescripcion(descripcion);
            notificacionServicio.insertar(notificacion);
        }
    }

    private Solicitud buscarSolicitudGestionable(Long idSolicitud) {
        if (idSolicitud == null || idSolicitud <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de solicitud inválido.");
        }
        Solicitud solicitud = solicitudRepositorio.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada."));
        verificarPropietario(solicitud.getMascota() == null ? null : solicitud.getMascota().getEntidad());
        if (!"EN_REVISION".equals(solicitud.getEstadoSolicitud())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya fue gestionada.");
        }
        return solicitud;
    }

    private void verificarEntidad(Long idEntidad) {
        if (idEntidad == null || idEntidad <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de entidad inválido.");
        }
        Entidad entidad = entidadRepositorio.findById(idEntidad)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entidad no encontrada."));
        verificarPropietario(entidad);
    }

    private Mascota verificarMascota(Long idMascota) {
        if (idMascota == null || idMascota <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador de mascota inválido.");
        }
        Mascota mascota = mascotaRepositorio.findById(idMascota)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mascota no encontrada."));
        verificarPropietario(mascota.getEntidad());
        return mascota;
    }

    private void verificarPropietario(Entidad entidad) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión inválida.");
        }
        boolean esEntidad = autenticacion.getAuthorities().stream()
                .anyMatch(rol -> "ROLE_ENTIDAD".equals(rol.getAuthority()));
        if (!esEntidad || entidad == null || entidad.getUsuario() == null
                || !autenticacion.getName().equals(entidad.getUsuario().getCorreo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes consultar o gestionar solicitudes de tu entidad.");
        }
    }

    private SolicitudDTO convertirParaEntidad(Solicitud solicitud) {
        SolicitudDTO dto = convertirADTO(solicitud);
        if (solicitud.getCiudadano() != null) {
            dto.setNombreSolicitante(solicitud.getCiudadano().getNombreCompleto());
            dto.setDistritoSolicitante(solicitud.getCiudadano().getDistrito());
        }
        if (solicitud.getMascota() != null) dto.setEspecieMascota(solicitud.getMascota().getEspecie());
        return dto;
    }

    private SolicitudDTO convertirADTO(Solicitud solicitud) {
        SolicitudDTO dto = modelMapper.map(
                solicitud,
                SolicitudDTO.class
        );

        if (solicitud.getMascota() != null) {
            dto.setMascotaIdMascota(
                    solicitud.getMascota().getIdMascota()
            );
        }

        if (solicitud.getCiudadano() != null) {
            dto.setCiudadanoIdCiudadano(
                    solicitud.getCiudadano().getIdCiudadano()
            );
        }

        return dto;
    }
}