package com.upc.huellasdeauxilio.servicios;

import com.upc.huellasdeauxilio.entidades.Ciudadano;
import com.upc.huellasdeauxilio.entidades.Usuario;
import com.upc.huellasdeauxilio.repositorios.CiudadanoRepositorio;
import com.upc.huellasdeauxilio.repositorios.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CiudadanoServicio {

    @Autowired
    private CiudadanoRepositorio ciudadanoRepositorio;

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    public Ciudadano insertar(Ciudadano ciudadano) {

        if (ciudadano == null
                || ciudadano.getUsuario() == null
                || ciudadano.getUsuario().getIdUsuario() == null) {
            return null;
        }

        Long idUsuario = ciudadano.getUsuario().getIdUsuario();

        Usuario usuario = usuarioRepositorio.findById(idUsuario)
                .orElse(null);

        if (usuario == null) {
            return null;
        }

        ciudadano.setUsuario(usuario);

        return ciudadanoRepositorio.save(ciudadano);
    }

    public Ciudadano buscarPorDni(String dni) {
        return ciudadanoRepositorio.findByDni(dni);
    }

    public Ciudadano buscarPorUsuario(Long idUsuario) {
        return ciudadanoRepositorio.findByUsuario_IdUsuario(idUsuario);
    }

    public Ciudadano obtenerPerfil(Long idCiudadano) {
        if (idCiudadano == null) {
            return null;
        }

        return ciudadanoRepositorio.findById(idCiudadano).orElse(null);
    }

    public Ciudadano actualizarPerfil(
            Long idCiudadano,
            Ciudadano datos
    ) {
        //validar los datos obligatorios
        if (idCiudadano == null
                || datos == null
                || datos.getNombreCompleto() == null
                || datos.getNombreCompleto().isBlank()
                || datos.getDni() == null
                || !datos.getDni().matches("\\d{8}")
                || datos.getDistrito() == null
                || datos.getDistrito().isBlank()) {

            return null;
        }

        Ciudadano ciudadano = obtenerPerfil(idCiudadano);

        if (ciudadano == null) {
            return null;
        }

        Ciudadano ciudadanoConDni = ciudadanoRepositorio.findByDni(datos.getDni());
        if (ciudadanoConDni != null
                && !ciudadanoConDni.getIdCiudadano().equals(idCiudadano)) {
            return null;
        }

        //actualizar únicamente los campos permitidos
        ciudadano.setNombreCompleto(datos.getNombreCompleto().trim());
        ciudadano.setDni(datos.getDni());
        ciudadano.setDistrito(datos.getDistrito().trim());

        return ciudadanoRepositorio.save(ciudadano);
    }
}
