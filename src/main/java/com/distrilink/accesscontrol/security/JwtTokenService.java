package com.distrilink.accesscontrol.security;

import com.distrilink.accesscontrol.model.Usuario;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Emite el token de acceso (D5). Ejemplo del contenido (payload):
 * <pre>
 * { "sub": "15", "email": "tienda@correo.com", "name": "Tienda Don Pepe",
 *   "role": "CLIENTE", "iss": "distrilink-access-control", "iat": ..., "exp": ... }
 * </pre>
 * "sub" es el id del usuario: los demás servicios lo toman de aquí, nunca de un parámetro.
 */
@Service
public class JwtTokenService {

    private final JwtEncoder encoder;
    private final String keyId;
    private final String issuer;
    private final Duration expiration;

    public JwtTokenService(JwtEncoder encoder, RSAKey rsaKey,
                           @Value("${app.jwt.issuer}") String issuer,
                           @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.encoder = encoder;
        this.keyId = rsaKey.getKeyID();
        this.issuer = issuer;
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(expiration))
                .claim("email", usuario.getCorreo())
                .claim("name", usuario.getNombre())
                .claim("role", usuario.getRol().name())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(keyId).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /** Segundos que dura el token (va en la respuesta del login como expiresIn). */
    public long segundosDeVida() {
        return expiration.toSeconds();
    }
}
