package com.upc.huellasdeauxilio.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionDTO {
    private Long idUbicacion;
    @NotBlank(message = "La dirección es obligatoria")
    @Size(min = 4, max = 200, message = "La dirección debe tener entre 4 y 200 caracteres")
    private String direccion;

    @NotBlank(message = "El distrito es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñ\\s]{2,100}$", message = "El distrito solo debe contener letras y espacios")
    private String distrito;

    @Size(max = 200, message = "La referencia no puede superar los 200 caracteres")
    private String referencia;

    @NotNull(message = "La latitud es obligatoria")
    private Float latitud;

    @NotNull(message = "La longitud es obligatoria")
    private Float longitud;
}
