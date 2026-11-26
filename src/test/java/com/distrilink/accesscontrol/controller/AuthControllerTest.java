package com.distrilink.accesscontrol.controller;

import com.distrilink.accesscontrol.config.CorsConfig;
import com.distrilink.accesscontrol.config.JwtKeyConfig;
import com.distrilink.accesscontrol.config.SecurityConfig;
import com.distrilink.accesscontrol.dto.LoginRequest;
import com.distrilink.accesscontrol.dto.LoginResponse;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.exception.AccountDisabledException;
import com.distrilink.accesscontrol.exception.InvalidCredentialsException;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import com.distrilink.accesscontrol.security.RestAccessDeniedHandler;
import com.distrilink.accesscontrol.security.RestAuthenticationEntryPoint;
import com.distrilink.accesscontrol.security.SecurityErrorWriter;
import com.distrilink.accesscontrol.service.AuthService;
import com.distrilink.accesscontrol.service.UsuarioService;
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

@WebMvcTest({AuthController.class, JwksController.class})
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, SecurityErrorWriter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioService usuarioService;

    private static final String LOGIN_JSON = """
            { "email": "tienda@correo.com", "password": "Clave1234" }
            """;

    private UsuarioResponse usuario() {
        return new UsuarioResponse(1L, "Tienda La Esquina", "tienda@correo.com", UsuarioRol.CUSTOMER,
                UsuarioEstado.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void login_correcto_devuelve200ConElToken() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("eyJ.token.firmado", "Bearer", 3600, usuario()));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("eyJ.token.firmado"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("tienda@correo.com"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist());
    }

    @Test
    void login_credencialesIncorrectas_devuelve401InvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value("Correo o contraseña incorrectos"));
    }

    @Test
    void login_cuentaInactiva_devuelve403AccountDisabled() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenThrow(new AccountDisabledException(UsuarioEstado.INACTIVE));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(LOGIN_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void login_sinCorreo_devuelve400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{ \"password\": \"x\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.email").exists());
    }

    @Test
    void me_conToken_devuelveElUsuarioDelToken() throws Exception {
        when(usuarioService.findById(1L)).thenReturn(usuario());

        mvc.perform(get("/api/auth/me").with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void me_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void jwks_esPublicoYSoloTraeLaLlavePublica() throws Exception {
        mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist()); // "d" sería la parte privada
    }
}
