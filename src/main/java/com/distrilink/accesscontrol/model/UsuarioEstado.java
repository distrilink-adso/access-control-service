package com.distrilink.accesscontrol.model;

/** Estado de la cuenta (RF1.7, RF1.12). Una cuenta nueva siempre nace ACTIVO. */
public enum UsuarioEstado {
    ACTIVO,
    INACTIVO,
    BLOQUEADO
}
