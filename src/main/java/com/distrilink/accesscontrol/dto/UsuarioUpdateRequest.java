package com.distrilink.accesscontrol.dto;

import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO de entrada para actualizar un usuario. La contraseña es opcional: si no se envía, no cambia. */
@Schema(description = "Datos para actualizar un usuario")
public record UsuarioUpdateRequest(

        @Schema(description = "Nombre completo o razón social del negocio", example = "Tienda La Esquina SAS")
        @NotBlank(message = "nombre es obligatorio")
        @Size(max = 120, message = "nombre admite máximo 120 caracteres")
        String nombre,

        @Schema(description = "Correo electrónico, único en el sistema", example = "tienda@correo.com")
        @NotBlank(message = "correo es obligatorio")
        @Email(message = "correo no tiene un formato válido")
        @Size(max = 150, message = "correo admite máximo 150 caracteres")
        String correo,

        @Schema(description = "Opcional. Si se envía, reemplaza la contraseña actual (8 a 72 caracteres, con letras y números)",
                example = "OtraClave99")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).{8,72}$",
                message = "contrasena debe tener entre 8 y 72 caracteres, con letras y números")
        String contrasena,

        @Schema(description = "Tipo de perfil", example = "CLIENTE")
        @NotNull(message = "rol es obligatorio (CLIENTE o DISTRIBUIDOR)")
        UsuarioRol rol,

        @Schema(description = "Estado de la cuenta", example = "ACTIVO")
        @NotNull(message = "estado es obligatorio (ACTIVO, INACTIVO o BLOQUEADO)")
        UsuarioEstado estado
) {
}
