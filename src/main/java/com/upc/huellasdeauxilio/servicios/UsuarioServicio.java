package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.entidades.Rol;
import com.upc.huellasdeauxilio.entidades.Usuario;
import com.upc.huellasdeauxilio.repositorios.RolRepositorio;
import com.upc.huellasdeauxilio.repositorios.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioServicio {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @Autowired
    private RolRepositorio rolRepositorio;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    public Usuario insertar(Usuario usuario) {

        if (usuario == null ||
                usuario.getRol() == null ||
                usuario.getRol().getIdRol() == null) {
            return null;
        }

        Rol rol = rolRepositorio
                .findById(usuario.getRol().getIdRol())
                .orElse(null);

        if (rol == null) {
            return null;
        }

        usuario.setRol(rol);
        usuario.setEstadoUsuario(true);

        String bcryptPassword = passwordEncoder.encode(usuario.getContraseña());
        usuario.setContraseña(bcryptPassword);

        return usuarioRepositorio.save(usuario);
    }

    public Usuario buscarPorId(Long idUsuario) {
        return usuarioRepositorio.findById(idUsuario).orElse(null);
    }

    public Usuario buscarPorCorreo(String correo) {
        return usuarioRepositorio.findByCorreo(correo);
    }


    public Usuario actualizarContacto(Long idUsuario, String correo, String telefono) {
        if (idUsuario == null
                || correo == null
                || correo.isBlank()
                || telefono == null
                || !telefono.matches("\\d{9}")) {
            return null;
        }

        String correoNormalizado =
                correo.trim().toLowerCase(java.util.Locale.ROOT);

        if (!correoNormalizado.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return null;
        }

        Usuario usuario = usuarioRepositorio.findById(idUsuario).orElse(null);
        if (usuario == null) {
            return null;
        }

        Usuario usuarioConCorreo = usuarioRepositorio.findByCorreo(correoNormalizado);
        if (usuarioConCorreo != null
                && !usuarioConCorreo.getIdUsuario().equals(idUsuario)) {
            return null;
        }

        usuario.setCorreo(correoNormalizado);
        usuario.setTelefono(telefono);

        return usuarioRepositorio.save(usuario);
    }

    public Usuario cambiarContrasena(
            String correo,
            String nuevaContrasena) {

        Usuario usuario =
                usuarioRepositorio.findByCorreo(correo);

        if (usuario != null) {
            usuario.setContraseña(passwordEncoder.encode(nuevaContrasena));
            return usuarioRepositorio.save(usuario);
        }

        return null;
    }

    //Eliminado Logico modificado para el funcionamiento con el frontend.
    public Usuario eliminadoLogico(
            String correo,
            String contraseña,
            Long id) {

        if (correo == null || contraseña == null || id == null) {
            return null;
        }

        Usuario datosIngresados = new Usuario();
        datosIngresados.setIdUsuario(id);
        datosIngresados.setCorreo(correo);
        datosIngresados.setContraseña(contraseña);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            datosIngresados.getCorreo(),
                            datosIngresados.getContraseña()
                    )
            );
        } catch (AuthenticationException error) {
            return null;
        }

        Usuario usuarioReal = usuarioRepositorio.findByCorreo(
                datosIngresados.getCorreo()
        );

        if (usuarioReal == null) {
            return null;
        }

        long idIngresado = datosIngresados.getIdUsuario();
        long idReal = usuarioReal.getIdUsuario();

        if (idIngresado != idReal) {
            return null;
        }

        usuarioReal.setEstadoUsuario(false);

        return usuarioRepositorio.save(usuarioReal);
    }

    public Usuario cambiarContrasenaDesdePerfil(
            String correo,
            String contrasenaActual,
            String nuevaContrasena,
            String confirmarContrasena) {

        if (correo == null || correo.isBlank() ||
                contrasenaActual == null || contrasenaActual.isBlank() ||
                nuevaContrasena == null || nuevaContrasena.isBlank() ||
                confirmarContrasena == null || confirmarContrasena.isBlank()) {

            return null;
        }

        Usuario usuario =
                usuarioRepositorio.findByCorreo(correo);

        if (usuario == null ||
                usuario.getContraseña() == null) {
            return null;
        }

        if (!passwordEncoder.matches(contrasenaActual, usuario.getContraseña())) {
            return null;
        }

        if (!nuevaContrasena.equals(confirmarContrasena)) {
            return null;
        }

        return cambiarContrasena(
                correo,
                nuevaContrasena
        );
    }
}
