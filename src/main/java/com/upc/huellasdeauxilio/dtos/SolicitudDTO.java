package com.upc.huellasdeauxilio.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDTO {
    private Long idSolicitud;
    @NotNull(message = "Debes seleccionar una mascota")
    @Positive(message = "El identificador de la mascota debe ser mayor que cero")
    private Long mascotaIdMascota;
    private Long ciudadanoIdCiudadano;

    private LocalDateTime fechaSolicitud;
    @NotBlank(message = "El tipo de vivienda es obligatorio")
    @Pattern(regexp = "^(casa|departamento|habitacion)$",
            message = "El tipo de vivienda debe ser casa, departamento o habitacion")
    private String tipoVivienda;
    @NotBlank(message = "El motivo de adopción es obligatorio y no puede contener solo espacios")
    @Size(max = 255, message = "El motivo de adopción no puede superar los 255 caracteres")
    private String motivo;
    @NotNull(message = "Debes indicar si tienes experiencia con mascotas")
    private Boolean experiencia;

    // Dato del formulario: se valida al registrar, sin almacenarlo en la entidad.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotNull(message = "Debes aceptar los términos de adopción responsable")
    @AssertTrue(message = "Debes aceptar los términos de adopción responsable")
    private Boolean aceptoTerminos;
    private String estadoSolicitud;

    // Información de consulta para las pantallas de entidad (no se acepta al registrar).
    private String nombreSolicitante;
    private String distritoSolicitante;
    private String especieMascota;
    private String dniSolicitante;
    private String correoSolicitante;
    private String telefonoSolicitante;


}
