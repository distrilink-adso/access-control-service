package com.distrilink.accesscontrol.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publica la llave PÚBLICA con la que se verifican los JWT (formato estándar JWKS).
 * catalog, order y notification la usan con:
 *   spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/.well-known/jwks.json
 * La llave privada nunca sale de este servicio.
 *
 * No usa ApiResponse a propósito: el formato JWKS es un estándar que leen las librerías de seguridad.
 */
@RestController
@Tag(name = "Llave pública", description = "JWKS para validar los tokens en los otros servicios")
public class JwksController {

    private final RSAKey rsaKey;

    public JwksController(RSAKey rsaKey) {
        this.rsaKey = rsaKey;
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "Llave pública para verificar los JWT")
    public Map<String, Object> jwks() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
