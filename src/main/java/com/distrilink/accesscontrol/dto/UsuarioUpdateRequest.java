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
        @NotBlank(message = "name es obligatorio")
        @Size(max = 120, message = "name admite máximo 120 caracteres")
        String name,

        @Schema(description = "Correo electrónico, único en el sistema", example = "tienda@correo.com")
        @NotBlank(message = "email es obligatorio")
        @Email(message = "email no tiene un formato válido")
        @Size(max = 150, message = "email admite máximo 150 caracteres")
        String email,

        @Schema(description = "Opcional. Si se envía, reemplaza la contraseña actual (8 a 72 caracteres, con letras y números)",
                example = "OtraClave99")
        @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\d).{8,72}$",
                message = "password debe tener entre 8 y 72 caracteres, con letras y números")
        String password,

        @Schema(description = "Tipo de perfil", example = "CUSTOMER")
        @NotNull(message = "role es obligatorio (CUSTOMER o DISTRIBUTOR)")
        UsuarioRol role,

        @Schema(description = "Estado de la cuenta", example = "ACTIVE")
        @NotNull(message = "status es obligatorio (ACTIVE, INACTIVE o LOCKED)")
        UsuarioEstado status
) {
}
