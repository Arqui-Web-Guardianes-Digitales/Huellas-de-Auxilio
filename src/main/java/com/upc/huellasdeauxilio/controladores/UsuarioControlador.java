package com.upc.huellasdeauxilio.controladores;

import com.upc.huellasdeauxilio.dtos.UsuarioDTO;
import com.upc.huellasdeauxilio.dtos.ContactoUsuarioDTO;
import com.upc.huellasdeauxilio.entidades.Rol;
import com.upc.huellasdeauxilio.entidades.Usuario;
import com.upc.huellasdeauxilio.servicios.UsuarioServicio;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class UsuarioControlador {

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private ModelMapper modelMapper;

    private Usuario convertirAEntidad(UsuarioDTO dto) {

        Usuario usuario =
                modelMapper.map(dto, Usuario.class);

        if (dto.getIdRol() != null) {

            Rol rol = new Rol();
            rol.setIdRol(dto.getIdRol());

            usuario.setRol(rol);
        }

        return usuario;
    }

    private UsuarioDTO convertirADTO(Usuario usuario) {

        if (usuario == null) {
            return null;
        }

        UsuarioDTO dto =
                modelMapper.map(usuario, UsuarioDTO.class);

        if (usuario.getRol() != null) {

            dto.setIdRol(
                    usuario.getRol().getIdRol()
            );
        }

        return dto;
    }

    // HU01 / HU02
    @PostMapping("/usuario")
    public UsuarioDTO insertar(
            @RequestBody UsuarioDTO usuarioDTO) {

        Usuario usuario =
                convertirAEntidad(usuarioDTO);

        usuario =
                usuarioServicio.insertar(usuario);

        return convertirADTO(usuario);
    }

    // HU01 / HU02 / HU04
    @GetMapping("/usuario/correo/{correo}")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'ENTIDAD')")
    public UsuarioDTO buscarPorCorreo(
            @PathVariable String correo) {

        Usuario usuario =
                usuarioServicio.buscarPorCorreo(correo);

        return convertirADTO(usuario);
    }

    //EndPoint creado para el desarrollo de la HU08
    @GetMapping("/usuario/{idUsuario}/contacto")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'ENTIDAD')")
    public UsuarioDTO buscarContactoPorId(
            @PathVariable Long idUsuario) {

        Usuario usuario = usuarioServicio.buscarPorId(idUsuario);

        if (usuario == null) {
            return null;
        }

        UsuarioDTO usuarioDTO = new UsuarioDTO();

        usuarioDTO.setIdUsuario(usuario.getIdUsuario());
        usuarioDTO.setTelefono(usuario.getTelefono());

        return usuarioDTO;
    }

    //ENDPOINT CREADO PARA LA ACTUALIZACION DE LOS CONTACTOS-HU19-22
    @PutMapping("/usuario/perfil/contacto/{idUsuario}")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'ENTIDAD')")
    public String actualizarContacto(
            @PathVariable Long idUsuario,
            @RequestBody ContactoUsuarioDTO contactoDTO) {

        Usuario actualizado = usuarioServicio.actualizarContacto(
                idUsuario,
                contactoDTO.getCorreo(),
                contactoDTO.getTelefono()
        );

        if (actualizado == null) {
            return "No se pudo actualizar el contacto";
        }

        return "Contacto actualizado correctamente";
    }

    // HU04
    @PutMapping("/usuario/contrasena/{correo}")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'ENTIDAD')")
    public UsuarioDTO cambiarContrasena(
            @PathVariable String correo,
            @RequestBody UsuarioDTO usuarioDTO) {

        Usuario usuario =
                usuarioServicio.cambiarContrasena(
                        correo,
                        usuarioDTO.getContraseña()
                );

        return convertirADTO(usuario);
    }

    //Eliminado Logico modificado para el funcionamiento con el frontend.
    @PutMapping("/usuario/usuario/{id}")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'ENTIDAD')")
    public UsuarioDTO eliminadoLogico(
            @RequestBody UsuarioDTO usuarioDTO,
            @PathVariable Long id) {

        Usuario usuario = usuarioServicio.eliminadoLogico(
                usuarioDTO.getCorreo(),
                usuarioDTO.getContraseña(),
                id
        );

        if (usuario == null) {
            return null;
        } else {
            UsuarioDTO respuesta = convertirADTO(usuario);
            respuesta.setContraseña(null);
            return respuesta;
        }
    }

    // HU21 / HU24 - ya existía
    @PutMapping("/usuario/perfil/contrasena/{correo}")
    @PreAuthorize("hasAnyRole('CIUDADANO', 'ENTIDAD')")
    public String cambiarContrasenaDesdePerfil(
            @PathVariable String correo,
            @RequestBody Map<String, String> datos) {

        if (datos == null) {
            return "Completa los datos para cambiar la contraseña.";
        }

        Usuario actualizado =
                usuarioServicio.cambiarContrasenaDesdePerfil(
                        correo,
                        datos.get("contrasenaActual"),
                        datos.get("nuevaContrasena"),
                        datos.get("confirmarContrasena")
                );

        if (actualizado == null) {
            return "No se pudo cambiar la contraseña. "
                    + "Verifica los datos ingresados.";
        }

        return "Contraseña actualizada correctamente";
    }
}
