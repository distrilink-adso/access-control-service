package com.distrilink.accesscontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** DTO de entrada del login. Los nombres de los campos son los mismos del registro (UsuarioRequest). */
@Schema(description = "Credenciales para iniciar sesión")
public record LoginRequest(

        @Schema(description = "Correo con el que se registró", example = "tienda@correo.com")
        @NotBlank(message = "correo es obligatorio")
        String correo,

        @Schema(description = "Contraseña", example = "Clave1234")
        @NotBlank(message = "contrasena es obligatoria")
        String contrasena
) {
}
