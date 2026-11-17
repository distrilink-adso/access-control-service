package com.distrilink.accesscontrol.controller;

import com.distrilink.accesscontrol.config.JwtConfig;
import com.distrilink.accesscontrol.config.SecurityConfig;
import com.distrilink.accesscontrol.dto.LoginRequest;
import com.distrilink.accesscontrol.dto.LoginResponse;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.exception.AccountDisabledException;
import com.distrilink.accesscontrol.exception.InvalidCredentialsException;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import com.distrilink.accesscontrol.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, JwksController.class})
@Import({SecurityConfig.class, JwtConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    private static final String LOGIN_JSON = """
            { "correo": "tienda@correo.com", "contrasena": "Clave1234" }
            """;

    private UsuarioResponse usuario() {
        return new UsuarioResponse(15L, "Tienda Don Pepe", "tienda@correo.com", UsuarioRol.CLIENTE,
                UsuarioEstado.ACTIVO, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void login_esPublicoYDevuelveElToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("token.firmado.rs256", "Bearer", 3600, usuario()));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token.firmado.rs256"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.usuario.id").value(15))
                .andExpect(jsonPath("$.usuario.contrasena").doesNotExist());
    }

    @Test
    void login_credencialesIncorrectas_devuelve401() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciales inválidas"))
                .andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos"));
    }

    @Test
    void login_cuentaBloqueada_devuelve403() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AccountDisabledException(UsuarioEstado.BLOQUEADO));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Cuenta no activa"));
    }

    @Test
    void login_sinCampos_devuelve400ConLosCampos() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.correo").exists())
                .andExpect(jsonPath("$.errors.contrasena").exists());
    }

    @Test
    void me_conToken_devuelveElUsuarioDelSub() throws Exception {
        when(authService.usuarioActual(15L)).thenReturn(usuario());

        mvc.perform(get("/api/auth/me").with(jwt().jwt(j -> j.subject("15"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15));
    }

    @Test
    void me_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwks_esPublicoYSoloTraeLaLlavePublica() throws Exception {
        mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").exists())
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].e").exists())
                // "d" es la parte privada: nunca debe publicarse.
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
    }
}
