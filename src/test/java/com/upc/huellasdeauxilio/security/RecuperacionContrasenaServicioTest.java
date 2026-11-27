package com.upc.huellasdeauxilio.security;

import com.upc.huellasdeauxilio.entidades.Usuario;
import com.upc.huellasdeauxilio.repositorios.UsuarioRepositorio;
import com.upc.huellasdeauxilio.security.entities.TokenRecuperacion;
import com.upc.huellasdeauxilio.security.repositories.TokenRecuperacionRepositorio;
import com.upc.huellasdeauxilio.security.services.RecuperacionContrasenaServicio;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecuperacionContrasenaServicioTest {
    private final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    private final TokenRecuperacionRepositorio tokens = mock(TokenRecuperacionRepositorio.class);
    private final JavaMailSender correo = mock(JavaMailSender.class);
    private final PasswordEncoder bcrypt = mock(PasswordEncoder.class);

    private RecuperacionContrasenaServicio servicio() {
        return new RecuperacionContrasenaServicio(
                usuarios, tokens, correo, bcrypt,
                "emisor@example.com", "http://localhost:4200/nueva-contrasena");
    }

    @Test
    void noEnviaSiElCorreoNoExiste() {
        when(usuarios.findByCorreo("inexistente@example.com")).thenReturn(null);
        servicio().solicitar("inexistente@example.com", "127.0.0.1");
        verifyNoInteractions(tokens, correo);
    }

    @Test
    void generaTokenAleatorioYNoGuardaElTokenEnClaro() {
        Usuario usuario = new Usuario();
        usuario.setCorreo("ciudadano@example.com");
        usuario.setEstadoUsuario(true);
        when(usuarios.findByCorreo("ciudadano@example.com")).thenReturn(usuario);
        when(tokens.findByUsuario(usuario)).thenReturn(List.of());
        when(tokens.saveAndFlush(any(TokenRecuperacion.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio().solicitar("ciudadano@example.com", "10.0.0.1");
        var tokenCapturado = org.mockito.ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokens).saveAndFlush(tokenCapturado.capture());
        var correoCapturado = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo).send(correoCapturado.capture());
        String cuerpo = correoCapturado.getValue().getText();
        assertNotNull(cuerpo);
        String tokenVisible = cuerpo.split("token=")[1].split("\\s")[0];
        assertEquals(43, tokenVisible.length());
        assertFalse(tokenCapturado.getValue().getTokenHash().contains(tokenVisible));
        assertEquals(64, tokenCapturado.getValue().getTokenHash().length());
        assertTrue(tokenCapturado.getValue().getVenceEn().isAfter(Instant.now().plusSeconds(29 * 60)));
    }

    @Test
    void rechazaTokenCaducadoOReutilizado() {
        String token = "a".repeat(43);
        var entidad = mock(TokenRecuperacion.class);
        when(entidad.getVenceEn()).thenReturn(Instant.now().minusSeconds(1));
        when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(entidad));
        assertFalse(servicio().restablecer(token, "UnaClaveSegura12", "UnaClaveSegura12"));
        verify(usuarios, never()).save(any(Usuario.class));
    }
}
