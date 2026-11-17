package com.distrilink.accesscontrol.exception;

/**
 * Correo o contraseña incorrectos. El mensaje es el mismo en los dos casos a propósito:
 * así nadie puede averiguar qué correos están registrados probando el login.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Correo o contraseña incorrectos");
    }
}
