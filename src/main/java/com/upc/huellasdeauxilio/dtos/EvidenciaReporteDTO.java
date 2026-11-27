package com.upc.huellasdeauxilio.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EvidenciaReporteDTO {
    private Long idEvidencia;
    @NotBlank(message = "La URL de la evidencia es obligatoria")
    @Size(max = 500, message = "La URL es demasiado larga")
    private String urlEvidencia;

    @NotNull(message = "El idReporte es obligatorio")
    private Long idReporte;
}
