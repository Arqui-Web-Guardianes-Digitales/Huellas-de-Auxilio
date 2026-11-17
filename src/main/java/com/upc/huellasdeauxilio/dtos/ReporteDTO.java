package com.upc.huellasdeauxilio.dtos;

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
    private String tipoCaso;
    private String nivelUrgencia;
    private String descripcion;
    private String estado;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String observacion;

    private UbicacionDTO ubicacion;
    private CiudadanoDTO ciudadano;
    private EntidadDTO entidad;
}
