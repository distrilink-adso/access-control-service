package com.distrilink.accesscontrol.controller;

import com.distrilink.accesscontrol.config.CorsConfig;
import com.distrilink.accesscontrol.config.JwtKeyConfig;
import com.distrilink.accesscontrol.config.SecurityConfig;
import com.distrilink.accesscontrol.dto.UsuarioRequest;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.exception.EmailAlreadyRegisteredException;
import com.distrilink.accesscontrol.exception.UsuarioNotFoundException;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import com.distrilink.accesscontrol.security.RestAccessDeniedHandler;
import com.distrilink.accesscontrol.security.RestAuthenticationEntryPoint;
import com.distrilink.accesscontrol.security.SecurityErrorWriter;
import com.distrilink.accesscontrol.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba del Controller con la seguridad real activada: el Service es un mock y
 * jwt() simula un token válido (sin firmarlo) con el id y el rol que se le indiquen.
 */
@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtKeyConfig.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, SecurityErrorWriter.class})
class UsuarioControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioService service;

    private static final String VALID_JSON = """
            {
              "nombre": "Tienda La Esquina",
              "correo": "tienda@correo.com",
              "contrasena": "Clave1234",
              "rol": "CLIENTE"
            }
            """;

    private UsuarioResponse response() {
        return new UsuarioResponse(1L, "Tienda La Esquina", "tienda@correo.com", UsuarioRol.CLIENTE,
                UsuarioEstado.ACTIVO, LocalDateTime.now(), LocalDateTime.now());
    }

    /** Simula que inició sesión el usuario con ese id y ese rol. */
    private static RequestPostProcessor usuario(long id, String rol) {
        return jwt().jwt(j -> j.subject(String.valueOf(id)).claim("role", rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    // ---------- Registro (público) ----------

    @Test
    void registro_sinToken_esPublicoYDevuelve201ConElWrapper() throws Exception {
        when(service.create(any(UsuarioRequest.class))).thenReturn(response());

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.data.contrasena").doesNotExist())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.meta.apiVersion").value("v1.0"));
    }

    @Test
    void registro_conVariosCamposInvalidos_devuelve400ConCadaCampo() throws Exception {
        String invalid = """
                { "nombre": "", "correo": "esto-no-es-un-correo", "contrasena": "corta1", "rol": "CLIENTE" }
                """;

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.nombre").exists())
                .andExpect(jsonPath("$.error.details.correo").exists())
                .andExpect(jsonPath("$.error.details.contrasena").exists());
    }

    @Test
    void registro_rolInexistente_devuelve400MalformedRequest() throws Exception {
        String invalid = """
                { "nombre": "Tienda", "correo": "tienda@correo.com", "contrasena": "Clave1234", "rol": "ADMIN" }
                """;

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void registro_correoRepetido_devuelve409() throws Exception {
        when(service.create(any(UsuarioRequest.class)))
                .thenThrow(new EmailAlreadyRegisteredException("tienda@correo.com"));

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    // ---------- Rutas protegidas ----------

    @Test
    void consultar_sinToken_devuelve401ConElWrapper() throws Exception {
        mvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void consultar_miPropiaCuenta_devuelve200() throws Exception {
        when(service.findById(1L)).thenReturn(response());

        mvc.perform(get("/api/users/1").with(usuario(1, "CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.correo").value("tienda@correo.com"));
    }

    @Test
    void consultar_laCuentaDeOtro_devuelve403() throws Exception {
        mvc.perform(get("/api/users/2").with(usuario(1, "CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(service, never()).findById(2L);
    }

    @Test
    void consultar_miCuentaQueYaNoExiste_devuelve404() throws Exception {
        when(service.findById(9L)).thenThrow(new UsuarioNotFoundException(9L));

        mvc.perform(get("/api/users/9").with(usuario(9, "CLIENTE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void listar_comoCliente_devuelve403() throws Exception {
        mvc.perform(get("/api/users").with(usuario(1, "CLIENTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void listar_comoDistribuidor_devuelve200ConLaLista() throws Exception {
        when(service.findAll(null, null)).thenReturn(List.of(response()));

        mvc.perform(get("/api/users").with(usuario(7, "DISTRIBUIDOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void desactivar_miCuenta_devuelve200ConDataNull() throws Exception {
        mvc.perform(delete("/api/users/1").with(usuario(1, "CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()));
        verify(service).delete(1L);
    }

    @Test
    void cors_preflightDesdeLiveServer_estaPermitidoSinToken() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/users/1")
                        .header("Origin", "http://127.0.0.1:5500")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5500"));
    }
}
