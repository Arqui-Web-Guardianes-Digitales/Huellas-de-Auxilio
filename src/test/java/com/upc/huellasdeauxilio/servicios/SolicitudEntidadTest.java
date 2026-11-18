package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.entidades.*;
import com.upc.huellasdeauxilio.repositorios.*;
import org.junit.jupiter.api.*;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

// Pruebas aisladas: no inicia Spring Boot ni accede a PostgreSQL.
class SolicitudEntidadTest {
    private SolicitudServicio servicio;
    private SolicitudRepositorio solicitudes;
    private MascotaRepositorio mascotas;
    private EntidadRepositorio entidades;
    private NotificacionServicio notificaciones;
    private MascotaServicio mascotaServicio;
    private Entidad entidad;
    private Mascota mascota;
    private Solicitud solicitud;
    @BeforeEach
    void preparar() {
        servicio = new SolicitudServicio();
        solicitudes = mock(SolicitudRepositorio.class);
        mascotas = mock(MascotaRepositorio.class);
        entidades = mock(EntidadRepositorio.class);
        notificaciones = mock(NotificacionServicio.class);
        mascotaServicio = mock(MascotaServicio.class);
        ReflectionTestUtils.setField(servicio, "solicitudRepositorio", solicitudes);
        ReflectionTestUtils.setField(servicio, "mascotaRepositorio", mascotas);
        ReflectionTestUtils.setField(servicio, "entidadRepositorio", entidades);
        ReflectionTestUtils.setField(servicio, "notificacionServicio", notificaciones);
        ReflectionTestUtils.setField(servicio, "mascotaServicio", mascotaServicio);
        ReflectionTestUtils.setField(servicio, "modelMapper", new ModelMapper());
        Usuario usuario = new Usuario(); usuario.setCorreo("entidad@prueba.local");
        entidad = new Entidad(); entidad.setIdEntidad(1L); entidad.setUsuario(usuario);
        mascota = new Mascota(); mascota.setIdMascota(7L); mascota.setEntidad(entidad);
        mascota.setEspecie("Gato"); mascota.setNombre("Luna"); mascota.setEstado(true);
        Usuario usuarioCiudadano = new Usuario(); usuarioCiudadano.setCorreo("ciudadano@prueba.local");
        usuarioCiudadano.setTelefono("999999999");
        Ciudadano ciudadano = new Ciudadano(); ciudadano.setIdCiudadano(3L); ciudadano.setNombreCompleto("Solicitante de prueba");
        ciudadano.setDistrito("San Miguel"); ciudadano.setDni("12345678"); ciudadano.setUsuario(usuarioCiudadano);
        solicitud = new Solicitud(); solicitud.setIdSolicitud(4L); solicitud.setMascota(mascota);
        solicitud.setCiudadano(ciudadano); solicitud.setEstadoSolicitud("EN_REVISION");
        solicitud.setFechaSolicitud(LocalDateTime.of(2026, 11, 17, 10, 0));
        when(entidades.findById(1L)).thenReturn(Optional.of(entidad));
        when(mascotas.findById(7L)).thenReturn(Optional.of(mascota));
        when(solicitudes.findById(4L)).thenReturn(Optional.of(solicitud));
        when(solicitudes.findByIdSolicitudAndMascota_IdMascota(4L, 7L)).thenReturn(solicitud);
        when(solicitudes.findByMascota_IdMascotaOrderByFechaSolicitudDesc(7L)).thenReturn(List.of(solicitud));
        when(solicitudes.findByMascota_Entidad_IdEntidadOrderByFechaSolicitudDesc(1L)).thenReturn(List.of(solicitud));
        when(solicitudes.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "entidad@prueba.local", null, List.of(new SimpleGrantedAuthority("ROLE_ENTIDAD"))));
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    @Test void listaSolicitudesPropiasConNombreDistritoYEspecieSinExponerContrasena() {
        var datos = servicio.listarPorMascota(7L);
        assertEquals(1, datos.size());
        assertEquals("Solicitante de prueba", datos.getFirst().getNombreSolicitante());
        assertEquals("San Miguel", datos.getFirst().getDistritoSolicitante());
        assertEquals("Gato", datos.getFirst().getEspecieMascota());
        assertNull(datos.getFirst().getDniSolicitante());
        assertEquals(1, servicio.listarPorEntidad(1L).size());
    }
    @Test void detallePropioIncluyeContactoDeSuSolicitante() {
        var dto = servicio.buscarPorSolicitudYMascota(4L, 7L);
        assertEquals("12345678", dto.getDniSolicitante());
        assertEquals("ciudadano@prueba.local", dto.getCorreoSolicitante());
        assertEquals("999999999", dto.getTelefonoSolicitante());
    }
    @Test void bloqueaListasFiltrosYDetalleDeOtraEntidad() {
        entidad.getUsuario().setCorreo("otra@prueba.local");
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> servicio.listarPorMascota(7L)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> servicio.listarPorEntidad(1L)).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> servicio.buscarPorSolicitudYMascota(4L, 7L));
        assertThrows(ResponseStatusException.class, () -> servicio.filtrarSolicitudesPorMascota(7L, "todos", "todos"));
        assertThrows(ResponseStatusException.class, () -> servicio.filtrarSolicitudesPorEntidad(1L, "todos", "todos", "todos"));
        verifyNoInteractions(notificaciones);
        verify(solicitudes, never()).save(any());
    }
    @Test void bloqueaAprobacionYRechazoAjenosAntesDeEscribir() {
        entidad.getUsuario().setCorreo("otra@prueba.local");
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> servicio.aprobarSolicitud(4L)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> servicio.rechazarSolicitud(4L)).getStatusCode().value());
        verify(solicitudes, never()).save(any());
        verifyNoInteractions(notificaciones, mascotaServicio);
    }
    @Test void aprobarRechazaOtrasPendientesRetiraMascotaYNotifica() {
        Solicitud otra = new Solicitud(); otra.setIdSolicitud(5L); otra.setMascota(mascota);
        otra.setCiudadano(solicitud.getCiudadano()); otra.setEstadoSolicitud("EN_REVISION");
        when(solicitudes.findByMascota_IdMascotaAndEstadoSolicitud(7L, "EN_REVISION")).thenReturn(List.of(otra));
        assertEquals("APROBADA", servicio.aprobarSolicitud(4L).getEstadoSolicitud());
        assertEquals("RECHAZADA", otra.getEstadoSolicitud());
        verify(mascotaServicio).cambiarEstado(7L, false);
        verify(solicitudes, times(2)).save(any());
        verify(notificaciones, times(2)).insertar(any());
    }
    @Test void rechazarSoloModificaLaSeleccionadaYNotifica() {
        assertEquals("RECHAZADA", servicio.rechazarSolicitud(4L).getEstadoSolicitud());
        verify(solicitudes, times(1)).save(solicitud);
        verify(solicitudes, never()).findByMascota_IdMascotaAndEstadoSolicitud(any(), any());
        verifyNoInteractions(mascotaServicio);
        verify(notificaciones).insertar(any());
    }
    @Test void noPermiteGestionarSolicitudesFinalizadas() {
        for (String estado : List.of("APROBADA", "RECHAZADA")) {
            solicitud.setEstadoSolicitud(estado);
            assertEquals(409, assertThrows(ResponseStatusException.class, () -> servicio.aprobarSolicitud(4L)).getStatusCode().value());
            assertEquals(409, assertThrows(ResponseStatusException.class, () -> servicio.rechazarSolicitud(4L)).getStatusCode().value());
        }
        verify(solicitudes, never()).save(any()); verifyNoInteractions(notificaciones, mascotaServicio);
    }
    @Test void noApruebaUnaMascotaQueYaNoEstaDisponible() {
        mascota.setEstado(false);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> servicio.aprobarSolicitud(4L)).getStatusCode().value());
        verify(solicitudes, never()).save(any()); verifyNoInteractions(notificaciones, mascotaServicio);
    }
    @Test void ciudadanoConservaSuConsultaSinAutorizacionDeEntidad() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "ciudadano@prueba.local", null, List.of(new SimpleGrantedAuthority("ROLE_CIUDADANO"))));
        when(solicitudes.findByIdSolicitudAndCiudadano_IdCiudadano(4L, 3L)).thenReturn(solicitud);
        var dto = servicio.buscarPorCodigoYCiudadano(4L, 3L);
        assertEquals(4L, dto.getIdSolicitud());
        assertEquals("EN_REVISION", dto.getEstadoSolicitud());
        assertNull(dto.getDniSolicitante());
        verifyNoInteractions(entidades);
    }

}
