package com.distrilink.accesscontrol.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publica la llave PÚBLICA en formato JWKS. catalog, order y notification la usan para verificar
 * los tokens; en su application.properties basta con:
 * <pre>
 * spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/.well-known/jwks.json
 * </pre>
 * La llave privada nunca sale de este servicio.
 */
@RestController
@Tag(name = "Autenticación")
public class JwksController {

    private final Map<String, Object> jwks;

    public JwksController(RSAKey rsaKey) {
        // toPublicJWKSet() quita la parte privada de la llave antes de publicarla.
        this.jwks = new JWKSet(rsaKey).toPublicJWKSet().toJSONObject();
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "Llave pública para verificar los tokens (JWKS)")
    public Map<String, Object> jwks() {
        return jwks;
    }
}
