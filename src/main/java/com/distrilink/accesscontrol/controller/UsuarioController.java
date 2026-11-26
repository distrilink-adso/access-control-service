package com.distrilink.accesscontrol.controller;

import com.distrilink.accesscontrol.dto.ApiResponse;
import com.distrilink.accesscontrol.dto.UsuarioRequest;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.dto.UsuarioUpdateRequest;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import com.distrilink.accesscontrol.security.CurrentUser;
import com.distrilink.accesscontrol.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Capa de Controllers: recibe HTTP, valida el DTO y delega en el Service.
 * Solo escribe el camino feliz: los errores los convierte GlobalExceptionHandler.
 *
 * Reglas de acceso (ver SecurityConfig):
 *  - POST /api/users          público (registro)
 *  - GET  /api/users          solo DISTRIBUTOR
 *  - GET/PUT/DELETE /{id}     solo el dueño de la cuenta (el id del token debe ser el mismo)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "CRUD de los usuarios de DistriLink (Clientes y Distribuidores)")
public class UsuarioController {

    private final UsuarioService service;

    @PostMapping
    @Operation(summary = "Registrar un usuario (público)",
            description = "Crea la cuenta ACTIVO con la contraseña cifrada. 409 si el correo ya existe")
    public ResponseEntity<ApiResponse<UsuarioResponse>> create(@Valid @RequestBody UsuarioRequest request) {
        UsuarioResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(ApiResponse.ok(created));
    }

    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Listar usuarios (solo DISTRIBUTOR)", description = "Los filtros por rol y estado son opcionales")
    public ResponseEntity<ApiResponse<List<UsuarioResponse>>> findAll(
            @Parameter(description = "Filtrar por rol", example = "CUSTOMER")
            @RequestParam(required = false) UsuarioRol rol,
            @Parameter(description = "Filtrar por estado", example = "ACTIVO")
            @RequestParam(required = false) UsuarioEstado estado) {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(rol, estado)));
    }

    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Consultar mi cuenta", description = "403 si el id no es el del usuario que inició sesión")
    public ResponseEntity<ApiResponse<UsuarioResponse>> findById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser.requireSelf(id, jwt);
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar mi cuenta", description = "La contraseña es opcional: si no se envía, se conserva la actual")
    public ResponseEntity<ApiResponse<UsuarioResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody UsuarioUpdateRequest request,
                                                               @AuthenticationPrincipal Jwt jwt) {
        CurrentUser.requireSelf(id, jwt);
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Desactivar mi cuenta",
            description = "No borra el registro: cambia el estado a INACTIVO (RF1.12). Responde 200 con data=null")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser.requireSelf(id, jwt);
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
