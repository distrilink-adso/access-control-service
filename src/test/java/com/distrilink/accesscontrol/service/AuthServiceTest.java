package com.distrilink.accesscontrol.service;

import com.distrilink.accesscontrol.dto.LoginRequest;
import com.distrilink.accesscontrol.dto.LoginResponse;
import com.distrilink.accesscontrol.exception.AccountDisabledException;
import com.distrilink.accesscontrol.exception.InvalidCredentialsException;
import com.distrilink.accesscontrol.model.Usuario;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import com.distrilink.accesscontrol.repository.UsuarioRepository;
import com.distrilink.accesscontrol.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba unitaria del login: repositorio, cifrador y emisor del token son mocks. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService tokenService;

    @InjectMocks
    private AuthService service;

    private Usuario usuario(UsuarioEstado estado) {
        Usuario u = new Usuario();
        u.setId(15L);
        u.setNombre("Tienda Don Pepe");
        u.setCorreo("tienda@correo.com");
        u.setContrasena("$2a$10$hashGuardado");
        u.setRol(UsuarioRol.CLIENTE);
        u.setEstado(estado);
        return u;
    }

    @Test
    void login_correcto_devuelveTokenYUsuarioSinContrasena() {
        Usuario u = usuario(UsuarioEstado.ACTIVO);
        when(repository.findByCorreo("tienda@correo.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Clave1234", "$2a$10$hashGuardado")).thenReturn(true);
        when(tokenService.generarToken(u)).thenReturn("token.firmado.rs256");
        when(tokenService.segundosDeVida()).thenReturn(3600L);

        // El correo llega con mayúsculas y espacios: se normaliza igual que en el registro.
        LoginResponse response = service.login(new LoginRequest("  TIENDA@Correo.com ", "Clave1234"));

        assertThat(response.accessToken()).isEqualTo("token.firmado.rs256");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.usuario().id()).isEqualTo(15L);
        assertThat(response.usuario().rol()).isEqualTo(UsuarioRol.CLIENTE);
    }

    @Test
    void login_contrasenaIncorrecta_lanza401SinToken() {
        when(repository.findByCorreo("tienda@correo.com")).thenReturn(Optional.of(usuario(UsuarioEstado.ACTIVO)));
        when(passwordEncoder.matches("Otra1234", "$2a$10$hashGuardado")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("tienda@correo.com", "Otra1234")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos");
        verify(tokenService, never()).generarToken(any());
    }

    @Test
    void login_correoInexistente_mismoErrorYTambienComparaLaContrasena() {
        when(repository.findByCorreo("nadie@correo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nadie@correo.com", "Clave1234")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Correo o contraseña incorrectos");
        // Compara contra el hash de relleno para que tarde lo mismo que con un correo real.
        verify(passwordEncoder).matches(eq("Clave1234"), anyString());
        verify(tokenService, never()).generarToken(any());
    }

    @Test
    void login_cuentaInactiva_lanza403SinToken() {
        when(repository.findByCorreo("tienda@correo.com")).thenReturn(Optional.of(usuario(UsuarioEstado.INACTIVO)));
        when(passwordEncoder.matches("Clave1234", "$2a$10$hashGuardado")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("tienda@correo.com", "Clave1234")))
                .isInstanceOf(AccountDisabledException.class)
                .hasMessageContaining("INACTIVO");
        verify(tokenService, never()).generarToken(any());
    }

    @Test
    void login_cuentaBloqueada_lanza403SinToken() {
        when(repository.findByCorreo("tienda@correo.com")).thenReturn(Optional.of(usuario(UsuarioEstado.BLOQUEADO)));
        when(passwordEncoder.matches("Clave1234", "$2a$10$hashGuardado")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("tienda@correo.com", "Clave1234")))
                .isInstanceOf(AccountDisabledException.class)
                .hasMessageContaining("BLOQUEADO");
    }
}
