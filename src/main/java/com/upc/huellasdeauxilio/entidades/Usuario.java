package com.upc.huellasdeauxilio.entidades;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;

    @ManyToOne
    @JoinColumn(name = "id_rol")
    private Rol rol;

    private String correo;
    private String telefono;
    private String contraseña;
    private Boolean estadoUsuario = true;

    // Revocacion de JWT despues de cambiar la contraseña. Nullable para no
    // romper usuarios anteriores a esta version (null equivale a cero).
    @Column(name = "version_sesion")
    private Integer versionSesion = 0;
}