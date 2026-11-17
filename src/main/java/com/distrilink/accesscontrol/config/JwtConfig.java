package com.distrilink.accesscontrol.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Llaves y piezas del JWT (D5 de la reunión de contratos).
 * <ul>
 *   <li>La llave PRIVADA solo la tiene este servicio: con ella firma los tokens (RS256).</li>
 *   <li>La llave PÚBLICA se publica en /.well-known/jwks.json: con ella catalog, order y
 *       notification verifican el token sin compartir ningún secreto.</li>
 * </ul>
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.private-key-path:}")
    private String privateKeyPath;

    @Value("${app.jwt.public-key-path:}")
    private String publicKeyPath;

    /** El par de llaves RSA. Lleva un "kid" (id de la llave) para que los otros servicios sepan cuál usar. */
    @Bean
    public RSAKey rsaKey() throws GeneralSecurityException, IOException, JOSEException {
        KeyPair keyPair;
        if (!privateKeyPath.isBlank() && !publicKeyPath.isBlank()) {
            keyPair = new KeyPair(leerPublica(Path.of(publicKeyPath)), leerPrivada(Path.of(privateKeyPath)));
            log.info("JWT: llaves RSA cargadas desde {} y {}", privateKeyPath, publicKeyPath);
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            keyPair = generator.generateKeyPair();
            log.warn("JWT: no se configuraron JWT_PRIVATE_KEY_PATH y JWT_PUBLIC_KEY_PATH. Se generó un par de "
                    + "llaves en memoria: los tokens dejan de valer cuando el servicio se reinicia.");
        }
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyIDFromThumbprint()
                .build();
    }

    /** Firma los tokens con la llave privada. */
    @Bean
    public JwtEncoder jwtEncoder(RSAKey rsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
    }

    /**
     * Valida los tokens que llegan a este mismo servicio (por ejemplo, a /api/users).
     * Revisa la firma, que no esté vencido y que el emisor sea access-control.
     */
    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    private static RSAPublicKey leerPublica(Path path) throws IOException, GeneralSecurityException {
        byte[] der = pemABytes(Files.readString(path));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    /** La privada debe estar en PKCS#8 ("-----BEGIN PRIVATE KEY-----"), como la genera el comando del README. */
    private static RSAPrivateKey leerPrivada(Path path) throws IOException, GeneralSecurityException {
        byte[] der = pemABytes(Files.readString(path));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private static byte[] pemABytes(String pem) {
        String base64 = pem.replaceAll("-----(BEGIN|END) [A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
