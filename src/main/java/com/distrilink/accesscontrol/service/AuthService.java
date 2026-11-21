package com.distrilink.accesscontrol.service;

import com.distrilink.accesscontrol.dto.LoginRequest;
import com.distrilink.accesscontrol.dto.LoginResponse;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.exception.AccountDisabledException;
import com.distrilink.accesscontrol.exception.InvalidCredentialsException;
import com.distrilink.accesscontrol.model.Usuario;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.repository.UsuarioRepository;
import com.distrilink.accesscontrol.security.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** RF1.4 y RF1.5: valida las credenciales y entrega el JWT con el rol del usuario. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);

        // Mismo error si el correo no existe o si la contraseña no coincide: no se revela qué correos están registrados.
        Usuario usuario = repository.findByCorreo(correo)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.contrasena(), usuario.getContrasena())) {
            throw new InvalidCredentialsException();
        }
        if (usuario.getEstado() != UsuarioEstado.ACTIVO) {
            throw new AccountDisabledException(usuario.getEstado());
        }

        String token = tokenService.createToken(usuario);
        return new LoginResponse(token, "Bearer", tokenService.expiresInSeconds(), UsuarioResponse.from(usuario));
    }
}
