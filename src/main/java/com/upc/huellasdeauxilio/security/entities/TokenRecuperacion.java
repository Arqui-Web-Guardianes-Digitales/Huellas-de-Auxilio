package com.upc.huellasdeauxilio.security.entities;

import com.upc.huellasdeauxilio.entidades.Usuario;
import jakarta.persistence.*;
import java.time.Instant;

/** Se almacena un SHA-256 del token, nunca el token que recibe el usuario. */
@Entity
@Table(name = "token_recuperacion", indexes = {
        @Index(name = "idx_token_recuperacion_usuario", columnList = "usuario_id")
})
public class TokenRecuperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "vence_en", nullable = false)
    private Instant venceEn;

    @Column(name = "utilizado_en")
    private Instant utilizadoEn;

    @Version
    private Long version;

    protected TokenRecuperacion() {}

    public TokenRecuperacion(Usuario usuario, String tokenHash, Instant venceEn) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.venceEn = venceEn;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getTokenHash() { return tokenHash; }
    public Instant getVenceEn() { return venceEn; }
    public Instant getUtilizadoEn() { return utilizadoEn; }
    public void setUtilizadoEn(Instant utilizadoEn) { this.utilizadoEn = utilizadoEn; }
}
