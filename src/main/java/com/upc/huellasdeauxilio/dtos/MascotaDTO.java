package com.upc.huellasdeauxilio.dtos;

import jakarta.validation.constraints.NotBlank;
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

public class MascotaDTO {
    private Long idMascota;
    private Long entidadIdEntidad;
    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 255, message = "El nombre admite hasta 255 caracteres.")
    private String nombre;
    @NotBlank(message = "La edad es obligatoria.")
    @Size(max = 255, message = "La edad admite hasta 255 caracteres.")
    private String edad;
    @NotBlank(message = "El sexo es obligatorio.")
    @Size(max = 255, message = "El sexo admite hasta 255 caracteres.")
    @Pattern(regexp = "(?iu)^\\s*(macho|hembra)\\s*$", message = "Selecciona una opción válida para sexo.")
    private String sexo;
    @NotBlank(message = "El distrito es obligatorio.")
    @Size(max = 255, message = "El distrito admite hasta 255 caracteres.")
    @Pattern(regexp = "(?iu)^\\s*(san miguel|miraflores|san borja|surco|jesús maría|pueblo libre|lince)\\s*$", message = "Selecciona una opción válida para distrito.")
    private String distrito;
    @NotBlank(message = "El tamaño es obligatorio.")
    @Size(max = 255, message = "El tamaño admite hasta 255 caracteres.")
    @Pattern(regexp = "(?iu)^\\s*(pequeño|mediano|grande)\\s*$", message = "Selecciona una opción válida para tamaño.")
    private String tamaño;
    private Boolean estado;
    @NotBlank(message = "La especie es obligatoria.")
    @Size(max = 255, message = "La especie admite hasta 255 caracteres.")
    @Pattern(regexp = "(?iu)^\\s*(perro|gato)\\s*$", message = "Selecciona una opción válida para especie.")
    private String especie;
    @NotBlank(message = "La fotografía es obligatoria.")
    @Size(max = 255, message = "La fotografía admite hasta 255 caracteres.")
    private String urlFoto;
    @NotBlank(message = "La descripción es obligatoria.")
    @Size(max = 255, message = "La descripción admite hasta 255 caracteres.")
    private String descripcion;
}
