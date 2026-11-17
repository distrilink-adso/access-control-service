package com.distrilink.accesscontrol.security;

import com.distrilink.accesscontrol.config.JwtConfig;
import com.distrilink.accesscontrol.model.Usuario;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba real de firma y verificación: genera un token con las llaves y lo vuelve a leer. */
class JwtTokenServiceTest {

    private static final String ISSUER = "distrilink-access-control";

    private JwtTokenService tokenService;
    private JwtDecoder decoder;

    private JwtConfig config() {
        JwtConfig config = new JwtConfig();
        ReflectionTestUtils.setField(config, "issuer", ISSUER);
        ReflectionTestUtils.setField(config, "privateKeyPath", "");
        ReflectionTestUtils.setField(config, "publicKeyPath", "");
        return config;
    }

    @BeforeEach
    void setUp() throws Exception {
        JwtConfig config = config();
        RSAKey rsaKey = config.rsaKey();
        tokenService = new JwtTokenService(config.jwtEncoder(rsaKey), rsaKey, ISSUER, 60);
        decoder = config.jwtDecoder(rsaKey);
    }

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(15L);
        u.setNombre("Tienda Don Pepe");
        u.setCorreo("tienda@correo.com");
        u.setContrasena("$2a$10$noDebeSalirEnElToken");
        u.setRol(UsuarioRol.DISTRIBUIDOR);
        u.setEstado(UsuarioEstado.ACTIVO);
        return u;
    }

    @Test
    void elTokenTraeLosClaimsAcordadosYDura60Minutos() {
        Jwt jwt = decoder.decode(tokenService.generarToken(usuario()));

        assertThat(jwt.getSubject()).isEqualTo("15");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("tienda@correo.com");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Tienda Don Pepe");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("DISTRIBUIDOR");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(60));
        assertThat(jwt.getHeaders()).containsEntry("alg", "RS256").containsKey("kid");
        assertThat(jwt.getClaims()).doesNotContainKey("contrasena");
        assertThat(tokenService.segundosDeVida()).isEqualTo(3600L);
    }

    @Test
    void unTokenFirmadoConOtraLlaveSeRechaza() throws Exception {
        // Otro "access-control" con otras llaves: su token no debe valer aquí.
        JwtConfig otro = config();
        RSAKey otraLlave = otro.rsaKey();
        String ajeno = new JwtTokenService(otro.jwtEncoder(otraLlave), otraLlave, ISSUER, 60).generarToken(usuario());

        assertThatThrownBy(() -> decoder.decode(ajeno)).isInstanceOf(JwtException.class);
    }

    @Test
    void unTokenModificadoSeRechaza() {
        String token = tokenService.generarToken(usuario());
        String[] partes = token.split("\\.");
        // Cambiar el payload (por ejemplo, para hacerse pasar por otro usuario) rompe la firma.
        String alterado = partes[0] + "." + partes[1].substring(0, partes[1].length() - 2) + "AA." + partes[2];

        assertThatThrownBy(() -> decoder.decode(alterado)).isInstanceOf(JwtException.class);
    }
}
