package com.upc.huellasdeauxilio.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporteDTO {
    private Long idReporte;
    private LocalDate fechaReporte;

    @NotBlank(message = "El tipo de caso es obligatorio")
    @Pattern(regexp = "^(M|A|R)$", message = "El tipo de caso debe ser M, A o R")
    private String tipoCaso;

    @NotBlank(message = "El nivel de urgencia es obligatorio")
    @Pattern(regexp = "^(AL|ME|BA)$", message = "El nivel de urgencia debe ser AL, ME o BA")
    private String nivelUrgencia;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(min = 5, max = 500, message = "La descripción debe tener entre 5 y 500 caracteres")
    private String descripcion;

    @Pattern(regexp = "^(R|ER|AT)$", message = "El estado debe ser R, ER o AT")
    private String estado;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String observacion;

    private UbicacionDTO ubicacion;
    private CiudadanoDTO ciudadano;
    private EntidadDTO entidad;
}
