package com.distrilink.accesscontrol.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Responde los 401 y 403 con el MISMO formato JSON que el resto del servicio (ProblemDetail).
 * <p>
 * Ojo: estos errores NO pasan por GlobalExceptionHandler (@RestControllerAdvice), porque Spring Security
 * los genera en un filtro, antes de llegar al controlador. Sin esta clase saldrían con el formato propio de Spring.
 * Cuando el equipo apruebe el ApiResponse (D4), solo se cambia el JSON de {@link #escribir}.
 */
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    /** 401: no mandó token, está vencido, la firma no coincide o el emisor no es access-control. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribir(request, response, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado",
                "Falta el token o no es válido. Inicia sesión en POST /api/auth/login y envía "
                        + "el header Authorization: Bearer <token>");
    }

    /** 403: el token es válido, pero ese usuario no tiene permiso para esto. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        escribir(request, response, HttpServletResponse.SC_FORBIDDEN, "Acceso denegado",
                "No tienes permiso para realizar esta acción");
    }

    private void escribir(HttpServletRequest request, HttpServletResponse response,
                          int status, String title, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = "{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\",\"instance\":\"" + escapar(request.getRequestURI()) + "\"}";
        response.getWriter().write(json);
    }

    /** La ruta la escribe el cliente: se escapan las comillas y las barras para no romper el JSON. */
    private static String escapar(String valor) {
        StringBuilder sb = new StringBuilder();
        for (char c : valor.toCharArray()) {
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
