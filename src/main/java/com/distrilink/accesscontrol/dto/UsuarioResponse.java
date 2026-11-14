package com.distrilink.accesscontrol.dto;

import com.distrilink.accesscontrol.model.Usuario;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** DTO de salida. Nunca incluye la contraseña ni su hash, y nunca se devuelve la @Entity directamente. */
@Schema(description = "Usuario devuelto por la API")
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        UsuarioRol rol,
        UsuarioEstado estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
                u.getId(), u.getNombre(), u.getCorreo(), u.getRol(), u.getEstado(),
                u.getFechaCreacion(), u.getFechaActualizacion());
    }
}
