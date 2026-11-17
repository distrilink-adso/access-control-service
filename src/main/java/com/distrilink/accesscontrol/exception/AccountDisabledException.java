package com.distrilink.accesscontrol.exception;

import com.distrilink.accesscontrol.model.UsuarioEstado;

/** La contraseña es correcta, pero la cuenta está INACTIVO o BLOQUEADO: no se entrega token. */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(UsuarioEstado estado) {
        super("La cuenta está " + estado.name() + " y no puede iniciar sesión");
    }
}
