package com.distrilink.accesscontrol.exception;

public class UsuarioNotFoundException extends RuntimeException {

    public UsuarioNotFoundException(Long id) {
        super("No existe el usuario con id " + id);
    }
}
