package com.distrilink.accesscontrol.exception;

/** RF1.2: ya existe otro usuario con ese correo. */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String correo) {
        super("El correo " + correo + " ya se encuentra registrado");
    }
}
