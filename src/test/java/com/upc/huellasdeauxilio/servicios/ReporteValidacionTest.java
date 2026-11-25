package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.controladores.*;
import com.upc.huellasdeauxilio.entidades.*;
import com.upc.huellasdeauxilio.repositorios.*;
import com.upc.huellasdeauxilio.excepciones.ValidacionControlador;
import org.junit.jupiter.api.*;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReporteValidacionTest {
    private ReporteServicio servicio;
    private ReporteRepositorio reportes;
    private EntidadRepositorio entidades;
    private ActualizarReporteServicio historial;
    private Entidad entidad;
    private Reporte reporte;
    @BeforeEach void preparar() {
        servicio = new ReporteServicio(); reportes = mock(ReporteRepositorio.class);
        entidades = mock(EntidadRepositorio.class); historial = new ActualizarReporteServicio();
        ReflectionTestUtils.setField(servicio, "reporteRepositorio", reportes);
        ReflectionTestUtils.setField(servicio, "entidadRepositorio", entidades);
        ReflectionTestUtils.setField(servicio, "actualizarReporteServicio", historial);
        Usuario usuario = new Usuario(); usuario.setCorreo("entidad@prueba.local");
        entidad = new Entidad(); entidad.setIdEntidad(1L); entidad.setUsuario(usuario);
        reporte = new Reporte(); reporte.setIdReporte(9L); reporte.setEstado("R"); reporte.setEntidad(entidad);
        when(entidades.findById(1L)).thenReturn(Optional.of(entidad));
        when(reportes.findById(9L)).thenReturn(Optional.of(reporte));
        when(reportes.buscarParaActualizar(9L)).thenReturn(Optional.of(reporte));
        when(reportes.findByEntidad_IdEntidadOrderByFechaReporteDesc(1L)).thenReturn(List.of(reporte));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "entidad@prueba.local", null, List.of(new SimpleGrantedAuthority("ROLE_ENTIDAD"))));
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    @Test void permiteListadoDetalleResumenYFiltrosPropios() {
        assertEquals(1, servicio.listarPorEntidad(1L).size());
        assertEquals(9L, servicio.buscarPorId(9L).getIdReporte());
        assertNotNull(servicio.obtenerResumenEntidad(1L));
        assertNotNull(servicio.filtrarReportesEntidad(1L, null, null, null));
    }
    @Test void bloqueaTodasLasConsultasDeOtraEntidad() {
        entidad.getUsuario().setCorreo("otra@prueba.local");
        assertEstado(403, () -> servicio.listarPorEntidad(1L));
        assertEstado(403, () -> servicio.obtenerResumenEntidad(1L));
        assertEstado(403, () -> servicio.filtrarReportesEntidad(1L, null, null, null));
        assertEstado(403, () -> servicio.buscarPorId(9L));
        verify(reportes, never()).findByEntidad_IdEntidadOrderByFechaReporteDesc(any());
    }
    @Test void distingueIdsInvalidosEInexistentes() {
        assertEstado(400, () -> servicio.listarPorEntidad(0L));
        assertEstado(404, () -> servicio.listarPorEntidad(999L));
        assertEstado(400, () -> servicio.buscarPorId(-1L));
        assertEstado(404, () -> servicio.buscarPorId(999L));
    }
    @Test void noCambiaLaConsultaCiudadana() {
        SecurityContextHolder.clearContext();
        servicio.listarPorCiudadano(3L); servicio.buscarPorCodigoYCiudadano(9L, 3L);
        verify(reportes).findByCiudadano_IdCiudadano(3L);
        verify(reportes).findByIdReporteAndCiudadano_IdCiudadano(9L, 3L);
        verifyNoInteractions(entidades);
    }
    @Test void hu06ConservaLaTransicionHistorialYNotificacion() {
        Ciudadano ciudadano = new Ciudadano(); ciudadano.setUsuario(new Usuario()); reporte.setCiudadano(ciudadano);
        var historialSimulado = mock(ActualizarReporteServicio.class);
        var avisos = mock(NotificacionServicio.class);
        ReflectionTestUtils.setField(servicio, "actualizarReporteServicio", historialSimulado);
        ReflectionTestUtils.setField(servicio, "notificacionServicio", avisos);
        when(reportes.save(reporte)).thenReturn(reporte);
        assertEquals("ER", servicio.actualizarEstado(9L, "ER", "Revisado").getEstado());
        verify(historialSimulado).verificarEntidad(reporte);
        verify(historialSimulado).insertar(any());
        verify(avisos, times(2)).insertar(any());
        assertEstado(409, () -> servicio.actualizarEstado(9L, "ER", ""));
        assertEstado(400, () -> servicio.actualizarEstado(9L, "R", ""));
    }
    @Test void httpMantiene403Y404EnDetalle() throws Exception {
        var controlador = new ReporteControlador();
        ReflectionTestUtils.setField(controlador, "reporteServicio", servicio);
        ReflectionTestUtils.setField(controlador, "modelMapper", new ModelMapper());
        var mvc = MockMvcBuilders.standaloneSetup(controlador).setControllerAdvice(new ValidacionControlador()).build();
        mvc.perform(get("/api/reportes_1/999")).andExpect(status().isNotFound());
        entidad.getUsuario().setCorreo("otra@prueba.local");
        mvc.perform(get("/api/reportes_1/9")).andExpect(status().isForbidden());
    }
    @Test void jakartaHU06NoExigeCamposDeRegistroYRechazaObservacionLarga() throws Exception {
        var controlador = new ReporteControlador(); var operaciones = mock(ReporteServicio.class);
        ReflectionTestUtils.setField(controlador, "reporteServicio", operaciones);
        ReflectionTestUtils.setField(controlador, "modelMapper", new ModelMapper());
        var mvc = MockMvcBuilders.standaloneSetup(controlador).setControllerAdvice(new ValidacionControlador()).build();
        mvc.perform(put("/api/reporte/estado/9").contentType("application/json").content("{\"estado\":\"ER\",\"observacion\":\"Revisión\"}"))
                .andExpect(status().isOk());
        verify(operaciones).actualizarEstado(9L, "ER", "Revisión");
        mvc.perform(put("/api/reporte/estado/9").contentType("application/json").content("{\"estado\":\"R\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(put("/api/reporte/estado/9").contentType("application/json").content("{\"estado\":\"AT\",\"observacion\":\""+"x".repeat(151)+"\"}"))
                .andExpect(status().isBadRequest());
    }
    @Test void manejadorConservaLosEstadosDeNegocio() {
        var manejador = new ValidacionControlador();
        for (int estado : List.of(400, 401, 403, 404, 409)) {
            assertEquals(estado, manejador.responderEstado(new ResponseStatusException(HttpStatus.valueOf(estado), "Mensaje de prueba")).getStatusCode().value());
        }
    }
    private void assertEstado(int estado, Runnable operacion) {
        assertEquals(estado, assertThrows(ResponseStatusException.class, operacion::run).getStatusCode().value());
    }
}
