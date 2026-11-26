package com.distrilink.accesscontrol.security;

import com.distrilink.accesscontrol.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Crea el JWT que recibe el usuario al iniciar sesión (contrato del equipo):
 *
 * <pre>
 * { "sub": "15", "email": "...", "name": "...", "role": "CUSTOMER",
 *   "iss": "distrilink-access-control", "iat": ..., "exp": ... }
 * </pre>
 *
 * Los otros servicios leen "sub" (id del usuario) y "role" del token; nunca reciben el id por parámetro.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final long expirationMinutes;

    public TokenService(JwtEncoder encoder,
                        @Value("${app.jwt.issuer}") String issuer,
                        @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public String createToken(Usuario usuario) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getCorreo())
                .claim("name", usuario.getNombre())
                .claim("role", usuario.getRol().name())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Segundos que dura el token: el frontend lo usa para saber cuándo pedir login otra vez. */
    public long expiresInSeconds() {
        return expirationMinutes * 60;
    }
}
