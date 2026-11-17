package com.distrilink.accesscontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta del login. accessToken, tokenType y expiresIn son los nombres estándar de OAuth 2,
 * los que esperan las librerías del frontend y de los otros servicios.
 */
@Schema(description = "Token de acceso y datos del usuario que inició sesión")
public record LoginResponse(

        @Schema(description = "JWT firmado con RS256. Se envía en el header Authorization: Bearer <token>")
        String accessToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(description = "Segundos que dura el token", example = "3600")
        long expiresIn,

        UsuarioResponse usuario
) {
}
