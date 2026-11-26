-- access-control-service: estados en ingles (ADR-007, seccion 6).
-- V1 y V2 no se editan: esta migracion renombra los valores que ya existen en la tabla.
UPDATE usuarios SET estado = 'ACTIVE'   WHERE estado = 'ACTIVO';
UPDATE usuarios SET estado = 'INACTIVE' WHERE estado = 'INACTIVO';
UPDATE usuarios SET estado = 'LOCKED'   WHERE estado = 'BLOQUEADO';
