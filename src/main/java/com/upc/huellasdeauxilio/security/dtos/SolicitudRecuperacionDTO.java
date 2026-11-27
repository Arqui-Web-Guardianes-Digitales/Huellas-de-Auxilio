package com.upc.huellasdeauxilio.security.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitudRecuperacionDTO(
        @NotBlank(message = "Ingresa tu correo.")
        @Email(message = "Ingresa un correo válido.") String correo) {}
