package com.distrilink.accesscontrol.model;

/** Estado de la cuenta (RF1.7, RF1.12). Una cuenta nueva siempre nace ACTIVE. Valores en inglés (ADR-007). */
public enum UsuarioEstado {
    ACTIVE,
    INACTIVE,
    LOCKED
}
