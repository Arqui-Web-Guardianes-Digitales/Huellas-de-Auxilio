package com.upc.huellasdeauxilio.security.controllers;

import com.upc.huellasdeauxilio.security.dtos.ConfirmacionRecuperacionDTO;
import com.upc.huellasdeauxilio.security.dtos.SolicitudRecuperacionDTO;
import com.upc.huellasdeauxilio.security.services.RecuperacionContrasenaServicio;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/password")
public class RecuperacionContrasenaControlador {
    private final RecuperacionContrasenaServicio servicio;

    public RecuperacionContrasenaControlador(RecuperacionContrasenaServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> solicitar(
            @Valid @RequestBody SolicitudRecuperacionDTO solicitud,
            HttpServletRequest request) {
        servicio.solicitar(solicitud.correo(), request.getRemoteAddr());
        return ResponseEntity.accepted().body(Map.of("mensaje",
                "Si el correo está registrado, recibirás un enlace de recuperación."));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> confirmar(
            @Valid @RequestBody ConfirmacionRecuperacionDTO confirmacion) {
        boolean actualizado = servicio.restablecer(
                confirmacion.token(), confirmacion.nuevaContrasena(), confirmacion.confirmarContrasena());
        if (!actualizado) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("mensaje", "El enlace no es válido, ya se usó o venció; o las contraseñas no coinciden."));
        }
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
    }
}
