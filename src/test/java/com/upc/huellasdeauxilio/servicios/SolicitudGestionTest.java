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
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

// Regresión HU30-HU32 con repositorios y notificaciones simulados.
class SolicitudGestionTest {
    private SolicitudServicio servicio;
    private SolicitudRepositorio solicitudes;
    private MascotaServicio mascotas;
    private NotificacionServicio avisos;
    private Entidad entidad;
    private Mascota mascota;
    private Solicitud solicitud;
    @BeforeEach void preparar() {
        servicio = new SolicitudServicio(); solicitudes = mock(SolicitudRepositorio.class);
        mascotas = mock(MascotaServicio.class); avisos = mock(NotificacionServicio.class);
        var repositorioMascotas = mock(MascotaRepositorio.class); var entidades = mock(EntidadRepositorio.class);
        ReflectionTestUtils.setField(servicio, "solicitudRepositorio", solicitudes);
        ReflectionTestUtils.setField(servicio, "mascotaServicio", mascotas);
        ReflectionTestUtils.setField(servicio, "mascotaRepositorio", repositorioMascotas);
        ReflectionTestUtils.setField(servicio, "entidadRepositorio", entidades);
        ReflectionTestUtils.setField(servicio, "notificacionServicio", avisos);
        ReflectionTestUtils.setField(servicio, "modelMapper", new ModelMapper());
        Usuario usuario = new Usuario(); usuario.setCorreo("entidad@prueba.local");
        usuario.setIdUsuario(1L);
        entidad = new Entidad(); entidad.setIdEntidad(1L); entidad.setUsuario(usuario);
        mascota = new Mascota(); mascota.setIdMascota(7L); mascota.setNombre("Luna"); mascota.setEstado(true); mascota.setEntidad(entidad);
        Ciudadano ciudadano = new Ciudadano(); ciudadano.setIdCiudadano(2L); ciudadano.setNombreCompleto("Solicitante de prueba"); ciudadano.setUsuario(new Usuario()); ciudadano.getUsuario().setIdUsuario(2L);
        solicitud = new Solicitud(); solicitud.setIdSolicitud(4L); solicitud.setMascota(mascota); solicitud.setCiudadano(ciudadano); solicitud.setEstadoSolicitud("EN_REVISION");
        when(entidades.findById(1L)).thenReturn(Optional.of(entidad));
        when(repositorioMascotas.findById(7L)).thenReturn(Optional.of(mascota));
        when(solicitudes.findById(4L)).thenReturn(Optional.of(solicitud));
        when(solicitudes.findByIdSolicitudAndMascota_IdMascota(4L, 7L)).thenReturn(solicitud);
        when(solicitudes.findByMascota_IdMascotaOrderByFechaSolicitudDesc(7L)).thenReturn(List.of(solicitud));
        when(solicitudes.findByMascota_Entidad_IdEntidadOrderByFechaSolicitudDesc(1L)).thenReturn(List.of(solicitud));
        when(solicitudes.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "entidad@prueba.local", null, List.of(new SimpleGrantedAuthority("ROLE_ENTIDAD"))));
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    @Test void mantieneConsultasDeMascotaEntidadYDetalle() {
        assertEquals(1, servicio.listarPorMascota(7L).size());
        assertEquals(1, servicio.listarPorEntidad(1L).size());
        assertEquals(4L, servicio.buscarPorSolicitudYMascota(4L,7L).getIdSolicitud());
    }
    @Test void noPermiteConsultarNiGestionarSolicitudesAjenas() {
        entidad.getUsuario().setCorreo("otra@prueba.local");
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> servicio.listarPorEntidad(1L)).getStatusCode().value());
        assertThrows(ResponseStatusException.class, () -> servicio.listarPorMascota(7L));
        assertThrows(ResponseStatusException.class, () -> servicio.aprobarSolicitud(4L));
        assertThrows(ResponseStatusException.class, () -> servicio.rechazarSolicitud(4L));
        verify(solicitudes, never()).save(any()); verifyNoInteractions(avisos, mascotas);
    }
    @Test void aprobarMantieneRechazoAutomaticoDisponibilidadYNotificaciones() {
        Solicitud otra = new Solicitud(); otra.setIdSolicitud(5L); otra.setMascota(mascota);
        otra.setCiudadano(solicitud.getCiudadano()); otra.setEstadoSolicitud("EN_REVISION");
        when(solicitudes.findByMascota_IdMascotaAndEstadoSolicitud(7L,"EN_REVISION")).thenReturn(List.of(otra));
        assertEquals("APROBADA",servicio.aprobarSolicitud(4L).getEstadoSolicitud());
        assertEquals("RECHAZADA",otra.getEstadoSolicitud());
        verify(mascotas).cambiarEstado(7L,false); verify(avisos, atLeast(2)).insertar(any());
    }
    @Test void rechazarNoCambiaLasDemasSolicitudesNiLaDisponibilidad() {
        assertEquals("RECHAZADA",servicio.rechazarSolicitud(4L).getEstadoSolicitud());
        verify(solicitudes,never()).findByMascota_IdMascotaAndEstadoSolicitud(any(),any());
        verifyNoInteractions(mascotas); verify(avisos, atLeastOnce()).insertar(any());
    }
    @Test void solicitudesFinalizadasYNoDisponiblesMantienen409() {
        solicitud.setEstadoSolicitud("APROBADA");
        assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.rechazarSolicitud(4L)).getStatusCode().value());
        solicitud.setEstadoSolicitud("EN_REVISION"); mascota.setEstado(false);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.aprobarSolicitud(4L)).getStatusCode().value());
        verify(solicitudes,never()).save(any());
    }
}
