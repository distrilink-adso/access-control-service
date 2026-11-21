package com.distrilink.accesscontrol.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

/** Ayudas para leer quién hizo la petición, a partir de su token. */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** Id del usuario que hizo la petición (claim "sub" del token). */
    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    /** 403 si el usuario intenta ver, cambiar o desactivar una cuenta que no es la suya. */
    public static void requireSelf(Long id, Jwt jwt) {
        if (!id.equals(id(jwt))) {
            throw new AccessDeniedException("Solo puedes consultar o modificar tu propia cuenta");
        }
    }
}
