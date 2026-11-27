package com.upc.huellasdeauxilio.security.util;

import com.upc.huellasdeauxilio.entidades.Usuario;
import com.upc.huellasdeauxilio.repositorios.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final UsuarioRepositorio usuarios;

    @Autowired
    public JwtUtil(@Value("${jwt.secret}") String secretKey, UsuarioRepositorio usuarios) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey.trim()));
        this.usuarios = usuarios;
    }

    // Conserva compatibilidad con pruebas unitarias sin contexto Spring ni PostgreSQL.
    public JwtUtil(String secretKey) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey.trim()));
        this.usuarios = null;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        if (usuarios != null) {
            Usuario usuario = usuarios.findByCorreo(userDetails.getUsername());
            if (usuario == null) throw new IllegalStateException("No se encontro el usuario del token");
            claims.put("sv", usuario.getVersionSesion() == null ? 0 : usuario.getVersionSesion());
        }
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder().claims(claims).subject(subject).issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 horas de duración
                .signWith(secretKey, Jwts.SIG.HS512).compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        if (!username.equals(userDetails.getUsername()) || isTokenExpired(token)) return false;
        if (usuarios == null) return true; // Solo pruebas unitarias sin base de datos.
        Usuario usuario = usuarios.findByCorreo(username);
        if (usuario == null || Boolean.FALSE.equals(usuario.getEstadoUsuario())) return false;
        Object versionJwt = extractAllClaims(token).get("sv");
        int versionActual = usuario.getVersionSesion() == null ? 0 : usuario.getVersionSesion();
        return versionJwt instanceof Number numero && numero.intValue() == versionActual;
    }
}
