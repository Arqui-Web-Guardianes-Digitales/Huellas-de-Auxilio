package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.dtos.MascotaDTO;
import com.upc.huellasdeauxilio.entidades.Entidad;
import com.upc.huellasdeauxilio.entidades.Mascota;
import com.upc.huellasdeauxilio.entidades.Usuario;
import com.upc.huellasdeauxilio.repositorios.EntidadRepositorio;
import com.upc.huellasdeauxilio.repositorios.MascotaRepositorio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolationException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Sin contexto de Spring ni conexión a PostgreSQL.
class MascotaPublicacionTest {
    private MascotaServicio servicio;
    private ValidatorFactory factory;
    private MascotaRepositorio mascotas;
    private EntidadRepositorio entidades;
    private MascotaDTO datos;

    @BeforeEach
    void preparar() {
        servicio = new MascotaServicio();
        factory = Validation.buildDefaultValidatorFactory();
        ReflectionTestUtils.setField(servicio, "validator", factory.getValidator());
        mascotas = mock(MascotaRepositorio.class);
        entidades = mock(EntidadRepositorio.class);
        ReflectionTestUtils.setField(servicio, "mascotaRepositorio", mascotas);
        ReflectionTestUtils.setField(servicio, "entidadRepositorio", entidades);
        ReflectionTestUtils.setField(servicio, "modelMapper", new ModelMapper());
        Usuario usuario = new Usuario();
        usuario.setCorreo("entidad@prueba.local");
        Entidad entidad = new Entidad();
        entidad.setIdEntidad(1L);
        entidad.setUsuario(usuario);
        when(entidades.findById(1L)).thenReturn(Optional.of(entidad));
        when(mascotas.save(any())).thenAnswer(invocacion -> {
            Mascota guardada = invocacion.getArgument(0);
            assertNull(guardada.getIdMascota());
            guardada.setIdMascota(10L);
            return guardada;
        });
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "entidad@prueba.local", null, List.of(new SimpleGrantedAuthority("ROLE_ENTIDAD"))));
        datos = new MascotaDTO();
        datos.setNombre("Nombre de prueba");
        datos.setEspecie("Gato");
        datos.setEdad("6 meses");
        datos.setSexo("Hembra");
        datos.setDistrito("San Miguel");
        datos.setTamaño("Pequeño");
        datos.setDescripcion("Descripción de prueba.");
        datos.setUrlFoto("http://localhost:8088/imagenes/foto-de-prueba.png");
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        factory.close();
    }

    @Test
    void publicaParaSuEntidadConEdadRealIdGeneradoYDisponible() {
        datos.setIdMascota(999L);
        datos.setEntidadIdEntidad(999L);
        datos.setEstado(false);
        MascotaDTO respuesta = servicio.publicarMascota(1L, datos);
        assertEquals(10L, respuesta.getIdMascota());
        assertEquals(1L, respuesta.getEntidadIdEntidad());
        assertEquals("6 meses", respuesta.getEdad());
        assertTrue(respuesta.getEstado());
        verify(mascotas, times(1)).save(any());
    }

    @Test
    void rechazaPublicacionParaOtraEntidad() {
        Usuario otroUsuario = new Usuario();
        otroUsuario.setCorreo("otra@prueba.local");
        Entidad otraEntidad = new Entidad();
        otraEntidad.setIdEntidad(2L);
        otraEntidad.setUsuario(otroUsuario);
        when(entidades.findById(2L)).thenReturn(Optional.of(otraEntidad));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> servicio.publicarMascota(2L, datos)).getStatusCode().value());
        verify(mascotas, never()).save(any());
    }

    @Test
    void distingueEntidadInexistenteYDatosInvalidos() {
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> servicio.publicarMascota(3L, datos)).getStatusCode().value());
        datos.setNombre("   ");
        assertThrows(ConstraintViolationException.class, () -> servicio.publicarMascota(1L, datos));
        verify(mascotas, never()).save(any());
    }

    @Test
    void rechazaEdadesYCatalogosInvalidos() {
        for (String edad : List.of("0 meses", "-1 año", "NaN años", "2 semanas", "1e999 años")) {
            datos.setEdad(edad);
            assertEquals(400, assertThrows(ResponseStatusException.class,
                    () -> servicio.publicarMascota(1L, datos)).getStatusCode().value());
        }
        datos.setEdad("6 meses");
        datos.setEspecie("Otra especie");
        assertThrows(ConstraintViolationException.class, () -> servicio.publicarMascota(1L, datos));
        datos.setEspecie("Gato");
        datos.setDistrito("Magdalena del Mar");
        assertThrows(ConstraintViolationException.class, () -> servicio.publicarMascota(1L, datos));
        verify(mascotas, never()).save(any());
    }

    @Test
    void conservaTextoDeEdadRealYCompatibilidadConCategoriasAnteriores() {
        for (String edad : List.of("1 mes", "1 año", "2 años", "1.5 años", "Adulto")) {
            datos.setEdad(edad);
            assertEquals(edad, servicio.publicarMascota(1L, datos).getEdad());
        }
    }

    @Test
    void editaSinCambiarEntidadDisponibilidadNiEdadCategorica() {
        Mascota mascota = new Mascota();
        mascota.setIdMascota(7L);
        mascota.setEntidad(entidades.findById(1L).orElseThrow());
        mascota.setEstado(false);
        when(mascotas.findById(7L)).thenReturn(Optional.of(mascota));
        doAnswer(invocacion -> invocacion.getArgument(0)).when(mascotas).save(any());
        datos.setEstado(true);
        datos.setEntidadIdEntidad(999L);
        datos.setEdad("Adulto");
        MascotaDTO respuesta = servicio.editarMascota(7L, datos);
        assertFalse(respuesta.getEstado());
        assertEquals(1L, respuesta.getEntidadIdEntidad());
        assertEquals("Adulto", respuesta.getEdad());
        assertEquals(datos.getUrlFoto(), respuesta.getUrlFoto());
    }

    @Test
    void impideEditarMascotaAjenaSinGuardar() {
        Mascota mascota = new Mascota();
        mascota.setEntidad(new Entidad());
        when(mascotas.findById(7L)).thenReturn(Optional.of(mascota));
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> servicio.editarMascota(7L, datos)).getStatusCode().value());
        verify(mascotas, never()).save(any());
    }

    @Test
    void exigeFotoYDescripcionValidas() {
        datos.setUrlFoto("");
        assertThrows(ConstraintViolationException.class, () -> servicio.publicarMascota(1L, datos));
        datos.setUrlFoto("http://localhost:8088/imagenes/foto-de-prueba.png");
        datos.setDescripcion("a".repeat(256));
        assertThrows(ConstraintViolationException.class, () -> servicio.publicarMascota(1L, datos));
        verify(mascotas, never()).save(any());
    }
}
