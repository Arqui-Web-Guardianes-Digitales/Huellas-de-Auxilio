package com.upc.huellasdeauxilio.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EstadoReporteDTO {
    @NotBlank(message = "Selecciona el siguiente estado del reporte.")
    @Pattern(regexp = "^(ER|AT)$", message = "El estado solicitado debe ser ER o AT.")
    private String estado;

    @Size(max = 150, message = "La observación admite hasta 150 caracteres.")
    private String observacion;
}
