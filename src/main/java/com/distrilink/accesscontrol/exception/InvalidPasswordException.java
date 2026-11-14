package com.distrilink.accesscontrol.exception;

/** La contraseña no se puede cifrar con BCrypt (pasa del límite de 72 bytes). */
public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException(String message) {
        super(message);
    }
}
