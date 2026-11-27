package com.upc.huellasdeauxilio.security.services;

import com.upc.huellasdeauxilio.entidades.Usuario;
import com.upc.huellasdeauxilio.repositorios.UsuarioRepositorio;
import com.upc.huellasdeauxilio.security.entities.TokenRecuperacion;
import com.upc.huellasdeauxilio.security.repositories.TokenRecuperacionRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RecuperacionContrasenaServicio {
    private static final Logger log = LoggerFactory.getLogger(RecuperacionContrasenaServicio.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Duration VIGENCIA = Duration.ofMinutes(30);
    private static final Duration ENFRIAMIENTO = Duration.ofMinutes(2);
    private static final int SOLICITUDES_IP_POR_HORA = 20;

    private final UsuarioRepositorio usuarios;
    private final TokenRecuperacionRepositorio tokens;
    private final JavaMailSender remitente;
    private final PasswordEncoder codificador;
    private final String remitenteCorreo;
    private final String enlaceFormulario;
    private final Map<String, Instant> ultimaSolicitudCorreo = new ConcurrentHashMap<>();
    private final Map<String, VentanaIp> ventanasIp = new ConcurrentHashMap<>();
    private final AtomicLong totalSolicitudes = new AtomicLong();

    private record VentanaIp(Instant inicio, int cantidad) {}

    public RecuperacionContrasenaServicio(
            UsuarioRepositorio usuarios, TokenRecuperacionRepositorio tokens,
            JavaMailSender remitente, PasswordEncoder codificador,
            @Value("${app.mail.from:}") String remitenteCorreo,
            @Value("${app.reset-url}") String enlaceFormulario) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.remitente = remitente;
        this.codificador = codificador;
        this.remitenteCorreo = remitenteCorreo;
        this.enlaceFormulario = enlaceFormulario;
    }

    // Siempre se devuelve un mensaje genérico; nunca revelar si existe un correo registrado.
    @Transactional
    public void solicitar(String correo, String ipCliente) {
        String normalizado = correo.trim().toLowerCase(Locale.ROOT);
        Instant ahora = Instant.now();
        if (!permitirIp(ipCliente, ahora) || !permitirCorreo(normalizado, ahora)) return;

        Usuario usuario = usuarios.findByCorreo(normalizado);
        if (usuario == null || Boolean.FALSE.equals(usuario.getEstadoUsuario())) return;
        if (remitenteCorreo.isBlank()) {
            log.warn("La recuperación no pudo enviarse: configura MAIL_FROM y MAIL_USERNAME.");
            return;
        }

        // Una solicitud posterior invalida los enlaces anteriores.
        tokens.deleteAll(tokens.findByUsuario(usuario));
        byte[] aleatorios = new byte[32];
        RANDOM.nextBytes(aleatorios);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorios);
        TokenRecuperacion persistido = tokens.saveAndFlush(
                new TokenRecuperacion(usuario, huella(token), ahora.plus(VIGENCIA)));
        String enlace = UriComponentsBuilder.fromUriString(enlaceFormulario)
                .queryParam("token", token)
                .build().encode().toUriString();

        SimpleMailMessage correoSalida = new SimpleMailMessage();
        correoSalida.setFrom(remitenteCorreo);
        correoSalida.setTo(usuario.getCorreo());
        correoSalida.setSubject("Huellas de Auxilio - Recuperar contraseña");
        correoSalida.setText("Hola,\n\nRecibimos una solicitud para cambiar tu contraseña en Huellas de Auxilio."
                + "\n\nAbre este enlace (válido durante 30 minutos y un solo uso):\n"
                + enlace
                + "\n\nSi no solicitaste este cambio, ignora el mensaje."
                + "\nNo compartas el enlace con nadie.\n\nEquipo Huellas de Auxilio");
        try {
            remitente.send(correoSalida);
        } catch (MailException e) {
            tokens.delete(persistido);
            // No revelar datos personales ni el enlace en los logs.
            log.warn("Falló el envío SMTP de recuperación ({}). Revisa la configuración del proveedor.",
                    e.getClass().getSimpleName());
        }
    }

    @Transactional
    public boolean restablecer(String token, String nueva, String confirmar) {
        if (token == null || !token.matches("^[A-Za-z0-9_-]{43}$") ||
                nueva == null || !nueva.equals(confirmar) ||
                nueva.getBytes(StandardCharsets.UTF_8).length < 8 ||
                nueva.getBytes(StandardCharsets.UTF_8).length > 72) return false;
        TokenRecuperacion existente = tokens.findByTokenHash(huella(token)).orElse(null);
        Instant ahora = Instant.now();
        if (existente == null || existente.getUtilizadoEn() != null ||
                !ahora.isBefore(existente.getVenceEn())) return false;
        Usuario usuario = existente.getUsuario();
        if (usuario == null || Boolean.FALSE.equals(usuario.getEstadoUsuario())) return false;
        usuario.setContraseña(codificador.encode(nueva));
        usuario.setVersionSesion((usuario.getVersionSesion() == null ? 0 : usuario.getVersionSesion()) + 1);
        usuarios.save(usuario);
        existente.setUtilizadoEn(ahora);
        tokens.saveAndFlush(existente); // @Version evita que dos peticiones lo consuman simultáneamente.
        return true;
    }

    private boolean permitirCorreo(String correo, Instant ahora) {
        AtomicBoolean permitido = new AtomicBoolean(false);
        String clave = huella(correo);
        ultimaSolicitudCorreo.compute(clave, (k, ultimo) -> {
            if (ultimo == null || ultimo.plus(ENFRIAMIENTO).isBefore(ahora)) {
                permitido.set(true);
                return ahora;
            }
            return ultimo;
        });
        limpiarLimites(ahora);
        return permitido.get();
    }

    private boolean permitirIp(String ip, Instant ahora) {
        AtomicBoolean permitido = new AtomicBoolean(false);
        String clave = huella(ip == null ? "desconocido" : ip);
        ventanasIp.compute(clave, (k, anterior) -> {
            if (anterior == null || anterior.inicio().plus(Duration.ofHours(1)).isBefore(ahora)) {
                permitido.set(true);
                return new VentanaIp(ahora, 1);
            }
            if (anterior.cantidad() < SOLICITUDES_IP_POR_HORA) {
                permitido.set(true);
                return new VentanaIp(anterior.inicio(), anterior.cantidad() + 1);
            }
            return anterior;
        });
        return permitido.get();
    }

    private void limpiarLimites(Instant ahora) {
        if (totalSolicitudes.incrementAndGet() % 200 != 0) return;
        ultimaSolicitudCorreo.entrySet().removeIf(e -> e.getValue().plus(ENFRIAMIENTO).isBefore(ahora));
        ventanasIp.entrySet().removeIf(e -> e.getValue().inicio().plus(Duration.ofHours(1)).isBefore(ahora));
    }

    private static String huella(String texto) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("El algoritmo SHA-256 no está disponible", ex);
        }
    }
}
