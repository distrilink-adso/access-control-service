package com.distrilink.accesscontrol.exception;

import com.distrilink.accesscontrol.model.UsuarioEstado;

/** La contraseña es correcta, pero la cuenta está INACTIVE o LOCKED (403). */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(UsuarioEstado status) {
        super("La cuenta está " + (status == UsuarioEstado.LOCKED ? "bloqueada" : "inactiva")
                + " y no puede iniciar sesión");
    }
}
