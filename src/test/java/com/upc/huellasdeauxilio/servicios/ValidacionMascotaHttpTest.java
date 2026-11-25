package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.controladores.*;
import com.upc.huellasdeauxilio.excepciones.ValidacionControlador;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ValidacionMascotaHttpTest {
    private final String datos = "{\"nombre\":\"Luna\",\"edad\":\"Adulto\",\"sexo\":\"Hembra\",\"distrito\":\"San Miguel\",\"tamaño\":\"Pequeño\",\"especie\":\"Gato\",\"urlFoto\":\"/imagenes/existente.png\",\"descripcion\":\"Descripción de prueba\"}";
    @Test void postYPutValidanCamposSinExigirIdEstadoNiEdadNumerica() throws Exception {
        var controlador = new MascotaControlador(); var servicio = mock(MascotaServicio.class);
        ReflectionTestUtils.setField(controlador, "mascotaServicio", servicio);
        var mvc = MockMvcBuilders.standaloneSetup(controlador).setControllerAdvice(new ValidacionControlador()).build();
        mvc.perform(post("/api/mascotas/entidad/1").contentType("application/json").content(datos)).andExpect(status().isOk());
        mvc.perform(put("/api/mascota/7").contentType("application/json").content(datos)).andExpect(status().isOk());
        verify(servicio).publicarMascota(eq(1L), any()); verify(servicio).editarMascota(eq(7L), any());
        mvc.perform(post("/api/mascotas/entidad/1").contentType("application/json").content(datos.replace("Luna", "   ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(put("/api/mascota/7").contentType("application/json").content(datos.replace("Gato", "Invalida")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/mascota/7").contentType("application/json").content(datos.replace("Descripción de prueba", "x".repeat(256))))
                .andExpect(status().isBadRequest());
        verifyNoMoreInteractions(servicio);
    }
    @Test void aceptaVariantesPreviasDeCatalogoSinCambiarElContrato() throws Exception {
        var controlador = new MascotaControlador(); var servicio = mock(MascotaServicio.class);
        ReflectionTestUtils.setField(controlador, "mascotaServicio", servicio);
        var mvc = MockMvcBuilders.standaloneSetup(controlador).setControllerAdvice(new ValidacionControlador()).build();
        mvc.perform(put("/api/mascota/7").contentType("application/json").content(datos.replace("Gato", " gato ").replace("Adulto", "1.5 años")))
                .andExpect(status().isOk());
    }
    @Test void validaContenidoYMIMEAntesDeGuardarSinEscribirArchivos() throws Exception {
        var controlador = new ArchivoControlador();
        for (String formato : new String[]{"png", "jpeg", "gif", "bmp"}) {
            var bytes = new ByteArrayOutputStream();
            assertTrue(ImageIO.write(new BufferedImage(1,1, BufferedImage.TYPE_INT_RGB), formato, bytes));
            var foto = new MockMultipartFile("archivo", "foto." + formato, "image/" + formato, bytes.toByteArray());
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(controlador, "validarImagen", foto));
        }
        var falso = new MockMultipartFile("archivo", "foto.png", "image/png", "texto".getBytes());
        assertThrows(ResponseStatusException.class, () -> ReflectionTestUtils.invokeMethod(controlador, "validarImagen", falso));
        var vacio = new MockMultipartFile("archivo", "foto.png", "image/png", new byte[0]);
        assertThrows(ResponseStatusException.class, () -> ReflectionTestUtils.invokeMethod(controlador, "validarImagen", vacio));
        var grande = new MockMultipartFile("archivo", "foto.png", "image/png", new byte[1024*1024+1]);
        var error = assertThrows(ResponseStatusException.class, () -> ReflectionTestUtils.invokeMethod(controlador, "validarImagen", grande));
        assertEquals(413, error.getStatusCode().value());
    }
    @Test void endpointImagenRechazaArchivoFalsoCon400() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ArchivoControlador()).setControllerAdvice(new ValidacionControlador()).build();
        mvc.perform(multipart("/api/archivos/subir").file(new MockMultipartFile("archivo", "foto.png", "image/png", "texto".getBytes())))
                .andExpect(status().isBadRequest());
    }
}
