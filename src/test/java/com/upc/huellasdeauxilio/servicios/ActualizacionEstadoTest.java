package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.entidades.*;
import com.upc.huellasdeauxilio.repositorios.ActualizarReporteRepositorio;
import com.upc.huellasdeauxilio.repositorios.ReporteRepositorio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Pruebas de servicios sin iniciar Spring Boot ni conectarse a PostgreSQL.
class ActualizacionEstadoTest {
    private ReporteRepositorio reportes;
    private ActualizarReporteRepositorio historial;
    private NotificacionServicio notificaciones;
    private ActualizarReporteServicio actualizaciones;
    private ReporteServicio servicio;
    private Reporte reporte;

    @BeforeEach
    void preparar() {
        reportes = mock(ReporteRepositorio.class);
        historial = mock(ActualizarReporteRepositorio.class);
        notificaciones = mock(NotificacionServicio.class);
        actualizaciones = new ActualizarReporteServicio();
        ReflectionTestUtils.setField(actualizaciones, "reporteRepositorio", reportes);
        ReflectionTestUtils.setField(actualizaciones, "actualizarReporteRepositorio", historial);
        servicio = new ReporteServicio();
        ReflectionTestUtils.setField(servicio, "reporteRepositorio", reportes);
        ReflectionTestUtils.setField(servicio, "actualizarReporteServicio", actualizaciones);
        ReflectionTestUtils.setField(servicio, "notificacionServicio", notificaciones);
        Usuario usuarioEntidad = new Usuario();
        usuarioEntidad.setCorreo("entidad@prueba.local");
        Entidad entidad = new Entidad();
        entidad.setUsuario(usuarioEntidad);
        Ciudadano ciudadano = new Ciudadano();
        Usuario usuarioCiudadano = new Usuario();
        usuarioCiudadano.setCorreo("ciudadano@prueba.local");
        ciudadano.setUsuario(usuarioCiudadano);
        reporte = new Reporte();
        reporte.setIdReporte(1L);
        reporte.setEntidad(entidad);
        reporte.setCiudadano(ciudadano);
        reporte.setEstado("R");
        when(reportes.buscarParaActualizar(1L)).thenReturn(Optional.of(reporte));
        when(reportes.findById(1L)).thenReturn(Optional.of(reporte));
        when(reportes.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(historial.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        autenticar("entidad@prueba.local", "ROLE_ENTIDAD");
    }

    private void autenticar(String correo, String rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(correo, null, List.of(new SimpleGrantedAuthority(rol))));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cambioValidoRegistraFechaObservacionYUnaNotificacion() {
        assertEquals("ER", servicio.actualizarEstado(1L, "ER", "  Se revisó el caso.  ").getEstado());
        var registro = org.mockito.ArgumentCaptor.forClass(ActualizarReporte.class);
        verify(historial, times(1)).save(registro.capture());
        assertNotNull(registro.getValue().getFechaActualizacion());
        assertEquals("Estado: R → ER. Observación: Se revisó el caso.", registro.getValue().getDescripcionActu());
        var aviso = org.mockito.ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones, times(1)).insertar(aviso.capture());
        assertSame(reporte.getCiudadano().getUsuario(), aviso.getValue().getUsuario());
        assertEquals(registro.getValue().getFechaActualizacion(), aviso.getValue().getFechaNotificacion());
    }

    @Test
    void permiteCerrarSinObservacion() {
        reporte.setEstado("ER");
        assertEquals("AT", servicio.actualizarEstado(1L, "AT", null).getEstado());
        verify(historial).save(any());
        verify(notificaciones).insertar(any());
    }

    @Test
    void rechazaSaltosRetrocesosRepeticionesYValoresInvalidos() {
        for (String estadoActual : List.of("R", "ER", "AT")) {
            for (String nuevoEstado : List.of("R", "ER", "AT", "OTRO", "")) {
                if ((estadoActual.equals("R") && nuevoEstado.equals("ER"))
                        || (estadoActual.equals("ER") && nuevoEstado.equals("AT"))) continue;
                reporte.setEstado(estadoActual);
                assertThrows(ResponseStatusException.class, () -> servicio.actualizarEstado(1L, nuevoEstado, ""));
                assertEquals(estadoActual, reporte.getEstado());
            }
        }
        assertThrows(ResponseStatusException.class, () -> servicio.actualizarEstado(1L, null, ""));
        verify(reportes, never()).save(any());
        verifyNoInteractions(historial, notificaciones);
    }

    @Test
    void repeticionTrasUnCambioNoGeneraOtroHistorialNiAviso() {
        servicio.actualizarEstado(1L, "ER", "");
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> servicio.actualizarEstado(1L, "ER", "")).getStatusCode().value());
        verify(historial, times(1)).save(any());
        verify(notificaciones, times(1)).insertar(any());
    }

    @Test
    void rechazaReporteAjenoAntesDeGuardar() {
        autenticar("otra@prueba.local", "ROLE_ENTIDAD");
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> servicio.actualizarEstado(1L, "ER", "")).getStatusCode().value());
        assertEquals("R", reporte.getEstado());
        verify(reportes, never()).save(any());
        verifyNoInteractions(historial, notificaciones);
    }

    @Test
    void rechazaReporteInexistenteYObservacionLarga() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> servicio.actualizarEstado(99L, "ER", "")).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> servicio.actualizarEstado(1L, "ER", "a".repeat(151))).getStatusCode().value());
        verifyNoInteractions(historial, notificaciones);
    }

    @Test
    void postHistorialRechazaEntidadAjenaEIdExistente() {
        ActualizarReporte registro = new ActualizarReporte();
        registro.setReporte(reporte);
        registro.setDescripcionActu("Observación de prueba.");
        autenticar("otra@prueba.local", "ROLE_ENTIDAD");
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> actualizaciones.insertar(registro)).getStatusCode().value());
        registro.setIdActualizacion(5L);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> actualizaciones.insertar(registro)).getStatusCode().value());
        verifyNoInteractions(historial);
    }

    @Test
    void consultaHistorialSoloParaEntidadOCiudadanoPropietarios() {
        actualizaciones.listarPorReporte(1L);
        autenticar("ciudadano@prueba.local", "ROLE_CIUDADANO");
        actualizaciones.listarPorReporte(1L);
        autenticar("otro@prueba.local", "ROLE_CIUDADANO");
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> actualizaciones.listarPorReporte(1L)).getStatusCode().value());
        verify(historial, times(2)).findByReporte_IdReporteOrderByFechaActualizacionAsc(1L);
    }
}
