package com.upc.huellasdeauxilio.security.repositories;

import com.upc.huellasdeauxilio.security.entities.TokenRecuperacion;
import com.upc.huellasdeauxilio.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TokenRecuperacionRepositorio extends JpaRepository<TokenRecuperacion, Long> {
    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);
    List<TokenRecuperacion> findByUsuario(Usuario usuario);
}
