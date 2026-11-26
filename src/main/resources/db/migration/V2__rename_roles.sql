-- access-control-service: roles en ingles (decision D1, ADR-007).
-- V1 no se edita: esta migracion renombra los valores que ya existen en la tabla.
UPDATE usuarios SET rol = 'CUSTOMER'    WHERE rol = 'CLIENTE';
UPDATE usuarios SET rol = 'DISTRIBUTOR' WHERE rol = 'DISTRIBUIDOR';
