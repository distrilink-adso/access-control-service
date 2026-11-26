package com.distrilink.accesscontrol.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** DTO de entrada para iniciar sesión (RF1.4). */
@Schema(description = "Credenciales para iniciar sesión")
public record LoginRequest(

        @Schema(description = "Correo registrado", example = "tienda@correo.com")
        @NotBlank(message = "correo es obligatorio")
        @Email(message = "correo no tiene un formato válido")
        String correo,

        @Schema(description = "Contraseña", example = "Clave1234")
        @NotBlank(message = "contrasena es obligatoria")
        String contrasena
) {
}
