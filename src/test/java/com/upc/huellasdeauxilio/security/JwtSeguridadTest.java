package com.upc.huellasdeauxilio.security;

import com.upc.huellasdeauxilio.security.util.JwtUtil;
import com.upc.huellasdeauxilio.security.controllers.AuthController;
import com.upc.huellasdeauxilio.security.filters.JwtRequestFilter;
import com.upc.huellasdeauxilio.security.services.CustomUserDetailsService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Sin servidor, contexto Spring ni PostgreSQL.
class JwtSeguridadTest {
    private final byte[] clave = new byte[64];
    private JwtUtil jwt;
    private UserDetails usuario;
    @BeforeEach void preparar() {
        Arrays.fill(clave, (byte) 7);
        jwt = new JwtUtil(Base64.getEncoder().encodeToString(clave));
        usuario = User.withUsername("entidad@prueba.local").password("no-utilizada").roles("ENTIDAD").build();
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }
    @Test void generaHS512CorreoYDiezHorasSinClaimsFicticios() {
        String token = jwt.generateToken(usuario);
        assertTrue(jwt.validateToken(token, usuario));
        var datos = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(clave)).build().parseSignedClaims(token);
        assertEquals("HS512", datos.getHeader().getAlgorithm());
        assertEquals(usuario.getUsername(), datos.getPayload().getSubject());
        assertEquals(36_000_000L, datos.getPayload().getExpiration().getTime() - datos.getPayload().getIssuedAt().getTime());
        assertFalse(datos.getPayload().containsKey("department"));
        assertFalse(datos.getPayload().containsKey("role"));
    }
    @Test void rechazaTokensMalformadosManipuladosYCaducados() {
        assertThrows(JwtException.class, () -> jwt.extractUsername("no-es-un-jwt"));
        String token = jwt.generateToken(usuario);
        String[] partes = token.split("\\.");
        partes[2] = (partes[2].charAt(0) == 'A' ? "B" : "A") + partes[2].substring(1);
        assertThrows(JwtException.class, () -> jwt.extractUsername(String.join(".", partes)));
        String expirado = Jwts.builder().subject(usuario.getUsername())
                .expiration(new Date(1)).signWith(Keys.hmacShaKeyFor(clave), Jwts.SIG.HS512).compact();
        assertThrows(ExpiredJwtException.class, () -> jwt.extractUsername(expirado));
    }
    @Test void aceptaFormatoHS512AnteriorConLaMismaClave() throws Exception {
        // Firma independiente con Java Crypto: mismo formato de los tokens anteriores.
        var codificador = Base64.getUrlEncoder().withoutPadding();
        String cabecera = codificador.encodeToString("{\"alg\":\"HS512\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String payload = "{\"sub\":\"entidad@prueba.local\",\"exp\":" + (System.currentTimeMillis()/1000+3600) + "}";
        String contenido = cabecera + "." + codificador.encodeToString(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var mac = javax.crypto.Mac.getInstance("HmacSHA512");
        mac.init(new javax.crypto.spec.SecretKeySpec(clave, "HmacSHA512"));
        assertTrue(jwt.validateToken(contenido + "." + codificador.encodeToString(mac.doFinal(contenido.getBytes(java.nio.charset.StandardCharsets.US_ASCII))), usuario));
    }
    @Test void filtroDevuelve401AnteTokenInvalido() throws Exception {
        var detalles = mock(CustomUserDetailsService.class);
        var filtro = new JwtRequestFilter(detalles, jwt);
        var request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer invalido");
        var response = new MockHttpServletResponse(); var cadena = new MockFilterChain();
        filtro.doFilter(request, response, cadena);
        assertEquals(401, response.getStatus()); assertNull(cadena.getRequest());
    }
    @Test void filtroAutenticaConLosRolesRealesDelUsuario() throws Exception {
        var detalles = mock(CustomUserDetailsService.class);
        when(detalles.loadUserByUsername(usuario.getUsername())).thenReturn(usuario);
        var filtro = new JwtRequestFilter(detalles, jwt);
        var request = new MockHttpServletRequest(); request.addHeader("Authorization", "Bearer " + jwt.generateToken(usuario));
        var response = new MockHttpServletResponse(); var cadena = new MockFilterChain();
        filtro.doFilter(request, response, cadena);
        assertEquals(usuario.getUsername(), SecurityContextHolder.getContext().getAuthentication().getName());
        assertNotNull(cadena.getRequest());
    }
    @Test void loginCiudadanoYEntidadMantienenHeaderCuerpoYRoles() throws Exception {
        var manager = mock(AuthenticationManager.class);
        var detalles = mock(CustomUserDetailsService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AuthController(manager, jwt, detalles)).addPlaceholderValue("ip.frontend", "http://localhost:4200").build();
        for (String rol : List.of("CIUDADANO", "ENTIDAD")) {
            UserDetails perfil = User.withUsername("cuenta@prueba.local").password("no-utilizada").roles(rol).build();
            when(detalles.loadUserByUsername("cuenta@prueba.local")).thenReturn(perfil);
            var respuesta = mvc.perform(post("/api/authenticate").contentType("application/json")
                    .content("{\"correo\":\"cuenta@prueba.local\",\"contraseña\":\"prueba\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("ROLE_" + rol)).andReturn().getResponse();
            String autorizacion = respuesta.getHeader("Authorization");
            assertNotNull(autorizacion); assertTrue(autorizacion.startsWith("Bearer "));
            assertTrue(jwt.validateToken(autorizacion.substring(7), perfil));
        }
    }
}
