-- access-control-service: tabla principal.
-- Hibernate NO crea esta tabla (ddl-auto=validate); la crea Flyway con este script.
-- Una migracion ya aplicada NUNCA se edita: para cambios se crea V2__..., V3__...

CREATE TABLE usuarios (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,   -- es el recipient_id que usa notification-service (sin FK: otra base de datos)
    nombre               VARCHAR(120)  NOT NULL,                  -- nombre completo o razon social del negocio
    correo               VARCHAR(150)  NOT NULL,                  -- unico, guardado en minusculas (RF1.2)
    contrasena           VARCHAR(255)  NOT NULL,                  -- hash BCrypt, nunca texto plano (RNF5.2)
    rol                  VARCHAR(20)   NOT NULL,                  -- CLIENTE, DISTRIBUIDOR
    estado               VARCHAR(20)   NOT NULL,                  -- ACTIVO, INACTIVO, BLOQUEADO
    fecha_creacion       DATETIME(6)   NOT NULL,
    fecha_actualizacion  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_correo UNIQUE (correo),
    INDEX idx_usuarios_rol_estado (rol, estado)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
