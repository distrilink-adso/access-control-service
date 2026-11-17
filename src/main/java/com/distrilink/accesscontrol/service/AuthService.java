package com.distrilink.accesscontrol.service;

import com.distrilink.accesscontrol.dto.LoginRequest;
import com.distrilink.accesscontrol.dto.LoginResponse;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.exception.AccountDisabledException;
import com.distrilink.accesscontrol.exception.InvalidCredentialsException;
import com.distrilink.accesscontrol.exception.UsuarioNotFoundException;
import com.distrilink.accesscontrol.model.Usuario;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.repository.UsuarioRepository;
import com.distrilink.accesscontrol.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/** RF1.4: inicio de sesión. Verifica correo y contraseña y entrega el JWT. */
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Hash BCrypt de una contraseña cualquiera. Si el correo no existe, se compara contra este hash
     * para que la respuesta tarde lo mismo que con un correo real (así no se puede adivinar qué correos existen).
     */
    private static final String HASH_DE_RELLENO = "$2a$10$/oYrj7oEKKvkoThdMEHPjOjofiBC9HeMlSEHzkYJsBwKjBTr.qO8O";

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> encontrado = repository.findByCorreo(correo);

        String hash = encontrado.map(Usuario::getContrasena).orElse(HASH_DE_RELLENO);
        boolean coincide = passwordEncoder.matches(request.contrasena(), hash);
        if (encontrado.isEmpty() || !coincide) {
            throw new InvalidCredentialsException();
        }

        Usuario usuario = encontrado.get();
        if (usuario.getEstado() != UsuarioEstado.ACTIVO) {
            throw new AccountDisabledException(usuario.getEstado());
        }

        return new LoginResponse(tokenService.generarToken(usuario), "Bearer",
                tokenService.segundosDeVida(), UsuarioResponse.from(usuario));
    }

    /** Datos del usuario dueño del token (GET /api/auth/me). */
    @Transactional(readOnly = true)
    public UsuarioResponse usuarioActual(Long id) {
        return repository.findById(id).map(UsuarioResponse::from).orElseThrow(() -> new UsuarioNotFoundException(id));
    }
}
