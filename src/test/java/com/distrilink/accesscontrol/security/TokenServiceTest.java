package com.distrilink.accesscontrol.security;

import com.distrilink.accesscontrol.config.JwtKeyConfig;
import com.distrilink.accesscontrol.model.Usuario;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Prueba real de firma y verificación: el token creado se valida con la llave pública. */
class TokenServiceTest {

    private TokenService tokenService;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        JwtKeyConfig config = new JwtKeyConfig();
        RSAKey key = config.rsaKey();
        tokenService = new TokenService(config.jwtEncoder(config.jwkSource(key)), "distrilink-access-control", 60);
        decoder = config.jwtDecoder(key);
    }

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(15L);
        u.setNombre("Tienda Don Pepe");
        u.setCorreo("tienda@donpepe.com");
        u.setRol(UsuarioRol.CUSTOMER);
        u.setEstado(UsuarioEstado.ACTIVO);
        return u;
    }

    @Test
    void elTokenTraeLosClaimsAcordadosYSeVerificaConLaLlavePublica() {
        Jwt jwt = decoder.decode(tokenService.createToken(usuario()));

        assertThat(jwt.getSubject()).isEqualTo("15");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("tienda@donpepe.com");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Tienda Don Pepe");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("CUSTOMER");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("distrilink-access-control");
        assertThat(jwt.getHeaders().get("alg")).isEqualTo("RS256");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
        assertThat(tokenService.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void unTokenModificadoEsRechazado() {
        String token = tokenService.createToken(usuario());
        String alterado = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> decoder.decode(alterado)).isInstanceOf(JwtException.class);
    }
}
