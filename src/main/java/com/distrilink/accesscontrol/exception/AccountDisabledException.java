package com.distrilink.accesscontrol.exception;

import com.distrilink.accesscontrol.model.UsuarioEstado;

/** La contraseña es correcta, pero la cuenta está INACTIVO o BLOQUEADO (403). */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(UsuarioEstado estado) {
        super("La cuenta está " + estado + " y no puede iniciar sesión");
    }
}
