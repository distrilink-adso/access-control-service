package com.distrilink.accesscontrol.exception;

import com.distrilink.accesscontrol.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * El "escudo" de la API: atrapa TODOS los errores lanzados por los controllers y services
 * y los convierte en un ApiResponse con success=false y el código HTTP que corresponde.
 * Los errores 401 y 403 que ocurren ANTES de llegar al controller (token faltante o inválido)
 * los responden RestAuthenticationEntryPoint y RestAccessDeniedHandler, con el mismo formato.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 400: uno o varios campos del DTO no cumplen sus reglas. Se listan TODOS en details. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> details.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                "Hay datos inválidos en la petición", details);
    }

    /** 400: JSON mal formado o un valor que no existe en un enum (por ejemplo, role "ADMIN"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST,
                "El cuerpo de la petición no es válido. Revisa el JSON y los valores permitidos "
                        + "(role: CUSTOMER o DISTRIBUTOR; status: ACTIVE, INACTIVE o LOCKED)", null);
    }

    /** 400: un parámetro de la URL no tiene el tipo esperado (por ejemplo, /api/users/abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PARAMETER,
                "El parámetro '" + ex.getName() + "' tiene un valor inválido",
                Map.of(ex.getName(), "Valor recibido no válido: " + ex.getValue()));
    }

    /** 400: la contraseña no se puede cifrar (pasa de 72 bytes). */
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPassword(InvalidPasswordException ex) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PASSWORD, ex.getMessage(),
                Map.of("password", ex.getMessage()));
    }

    /** 401: correo o contraseña incorrectos en el login. */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, ex.getMessage(), null);
    }

    /** 403: la cuenta existe pero está INACTIVE o LOCKED. */
    @ExceptionHandler(AccountDisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountDisabled(AccountDisabledException ex) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED, ex.getMessage(), null);
    }

    /** 403: el usuario tiene sesión, pero no puede tocar ese recurso (por ejemplo, el perfil de otro). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, ex.getMessage(), null);
    }

    /** 404: el usuario pedido no existe. */
    @ExceptionHandler(UsuarioNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(UsuarioNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND, ex.getMessage(), null);
    }

    /** 404: la ruta no existe en la API. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoEndpoint(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.ENDPOINT_NOT_FOUND, "La ruta solicitada no existe", null);
    }

    /** 405: la ruta existe pero no acepta ese método HTTP. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED,
                "El método " + ex.getMethod() + " no está permitido en esta ruta", null);
    }

    /** 409: el correo ya está registrado (RF1.2). */
    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailDuplicado(EmailAlreadyRegisteredException ex) {
        return build(HttpStatus.CONFLICT, ErrorCode.EMAIL_ALREADY_REGISTERED, ex.getMessage(),
                Map.of("email", ex.getMessage()));
    }

    /** 409: red de seguridad si dos registros simultáneos con el mismo correo chocan en la restricción UNIQUE. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegridad(DataIntegrityViolationException ex) {
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLICT,
                "Los datos enviados chocan con un registro existente (por ejemplo, un correo repetido)", null);
    }

    /** 500: cualquier otro error. Se registra en el log, pero el cliente NO ve el detalle técnico. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Ocurrió un error inesperado. Intenta de nuevo más tarde", null);
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String code, String message,
                                                    Map<String, String> details) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message, details));
    }
}
