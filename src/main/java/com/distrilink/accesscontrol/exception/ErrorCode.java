package com.distrilink.accesscontrol.exception;

/**
 * Códigos de error de DistriLink. Los comunes son iguales en los 4 microservicios
 * para que el frontend los reconozca sin importar qué servicio respondió.
 */
public final class ErrorCode {

    // ----- Comunes a los 4 servicios -----
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";       // 400: campos inválidos
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";     // 400: JSON mal escrito o valor de enum inexistente
    public static final String INVALID_PARAMETER = "INVALID_PARAMETER";     // 400: parámetro de la URL con tipo incorrecto
    public static final String UNAUTHORIZED = "UNAUTHORIZED";               // 401: sin token o token vencido
    public static final String FORBIDDEN = "FORBIDDEN";                     // 403: sin permiso para la acción
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";   // 404: el recurso no existe
    public static final String ENDPOINT_NOT_FOUND = "ENDPOINT_NOT_FOUND";   // 404: la ruta no existe
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";   // 405: método HTTP no soportado
    public static final String CONFLICT = "CONFLICT";                       // 409: choca con datos existentes
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";           // 500: fallo no controlado

    // ----- Propios de access-control-service -----
    public static final String EMAIL_ALREADY_REGISTERED = "EMAIL_ALREADY_REGISTERED"; // 409: correo repetido (RF1.2)
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";                 // 400: contraseña no cifrable
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";           // 401: correo o contraseña incorrectos
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";                 // 403: cuenta INACTIVO o BLOQUEADO

    private ErrorCode() {
    }
}
