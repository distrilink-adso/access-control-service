package com.distrilink.accesscontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** DTO de salida del login: el token y los datos básicos del usuario (sin contraseña). */
@Schema(description = "Resultado de un login exitoso")
public record LoginResponse(

        @Schema(description = "JWT para enviar en el header Authorization: Bearer <token>")
        String accessToken,

        @Schema(description = "Tipo de token", example = "Bearer")
        String tokenType,

        @Schema(description = "Segundos de vida del token", example = "3600")
        long expiresIn,

        @Schema(description = "Usuario que inició sesión")
        UsuarioResponse usuario
) {
}
