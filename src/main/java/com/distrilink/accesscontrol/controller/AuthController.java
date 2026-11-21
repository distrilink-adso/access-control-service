package com.distrilink.accesscontrol.controller;

import com.distrilink.accesscontrol.dto.ApiResponse;
import com.distrilink.accesscontrol.dto.LoginRequest;
import com.distrilink.accesscontrol.dto.LoginResponse;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.security.CurrentUser;
import com.distrilink.accesscontrol.service.AuthService;
import com.distrilink.accesscontrol.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login (RF1.4) y "quién soy" a partir del token. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT")
public class AuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Devuelve el JWT. 401 si el correo o la contraseña son incorrectos; 403 si la cuenta no está ACTIVO")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Usuario que inició sesión", description = "Lee el id del token; no recibe parámetros")
    public ResponseEntity<ApiResponse<UsuarioResponse>> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.findById(CurrentUser.id(jwt))));
    }
}
