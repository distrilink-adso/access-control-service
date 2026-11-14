package com.distrilink.accesscontrol.controller;

import com.distrilink.accesscontrol.dto.UsuarioRequest;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.dto.UsuarioUpdateRequest;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import com.distrilink.accesscontrol.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Capa de Controllers: recibe HTTP, valida el DTO y delega en el Service. */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "CRUD de los usuarios de DistriLink (Clientes y Distribuidores)")
public class UsuarioController {

    private final UsuarioService service;

    @PostMapping
    @Operation(summary = "Registrar un usuario",
            description = "Crea la cuenta ACTIVO con la contraseña cifrada. Devuelve 409 si el correo ya existe")
    public ResponseEntity<UsuarioResponse> create(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Los filtros por rol y estado son opcionales")
    public List<UsuarioResponse> findAll(
            @Parameter(description = "Filtrar por rol", example = "CLIENTE")
            @RequestParam(required = false) UsuarioRol rol,
            @Parameter(description = "Filtrar por estado", example = "ACTIVO")
            @RequestParam(required = false) UsuarioEstado estado) {
        return service.findAll(rol, estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un usuario por id")
    public UsuarioResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario",
            description = "La contraseña es opcional: si no se envía, se conserva la actual")
    public UsuarioResponse update(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar un usuario",
            description = "No borra el registro: cambia el estado a INACTIVO y conserva la información (RF1.12)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
