package com.upc.huellasdeauxilio.security.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmacionRecuperacionDTO(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String nuevaContrasena,
        @NotBlank String confirmarContrasena) {}
