package com.distrilink.accesscontrol.exception;

/**
 * Correo o contraseña incorrectos en el login (401).
 * El mensaje es el mismo en los dos casos a propósito: así nadie puede averiguar qué correos existen.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Correo o contraseña incorrectos");
    }
}
