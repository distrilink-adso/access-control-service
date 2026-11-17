package com.distrilink.accesscontrol.controller;

import com.distrilink.accesscontrol.config.JwtConfig;
import com.distrilink.accesscontrol.config.SecurityConfig;
import com.distrilink.accesscontrol.dto.UsuarioRequest;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.exception.EmailAlreadyRegisteredException;
import com.distrilink.accesscontrol.exception.UsuarioNotFoundException;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
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

/** Prueba del Controller: solo levanta la capa web y la seguridad; el Service es un mock. */
@WebMvcTest(UsuarioController.class)
@Import({SecurityConfig.class, JwtConfig.class})
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

    @Test
    void post_valido_devuelve201ConElDtoSinContrasena() throws Exception {
        when(service.create(any(UsuarioRequest.class))).thenReturn(response());

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.contrasena").doesNotExist());
    }

    @Test
    void post_contrasenaDebil_devuelve400ConElCampo() throws Exception {
        String invalid = """
                { "nombre": "Tienda", "correo": "tienda@correo.com", "contrasena": "corta1", "rol": "CLIENTE" }
                """;

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.contrasena").exists());
    }

    @Test
    void post_correoInvalido_devuelve400ConElCampo() throws Exception {
        String invalid = """
                { "nombre": "Tienda", "correo": "esto-no-es-un-correo", "contrasena": "Clave1234", "rol": "CLIENTE" }
                """;

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.correo").exists());
    }

    @Test
    void post_rolInexistente_devuelve400() throws Exception {
        String invalid = """
                { "nombre": "Tienda", "correo": "tienda@correo.com", "contrasena": "Clave1234", "rol": "ADMIN" }
                """;

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Petición inválida"));
    }

    @Test
    void post_correoRepetido_devuelve409() throws Exception {
        when(service.create(any(UsuarioRequest.class)))
                .thenThrow(new EmailAlreadyRegisteredException("tienda@correo.com"));

        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Correo ya registrado"));
    }

    @Test
    void get_inexistente_devuelve404() throws Exception {
        when(service.findById(99L)).thenThrow(new UsuarioNotFoundException(99L));

        mvc.perform(get("/api/users/99").with(jwt().jwt(j -> j.subject("99"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Usuario no encontrado"));
    }

    @Test
    void delete_devuelve204() throws Exception {
        mvc.perform(delete("/api/users/1").with(jwt().jwt(j -> j.subject("1"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void get_sinToken_devuelve401ConFormatoProblemDetail() throws Exception {
        mvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.title").value("No autenticado"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void get_tokenInventado_devuelve401() throws Exception {
        mvc.perform(get("/api/users/1").header("Authorization", "Bearer esto.no.es-un-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void get_deOtroUsuario_devuelve403() throws Exception {
        mvc.perform(get("/api/users/1").with(jwt().jwt(j -> j.subject("2"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acceso denegado"));
    }

    @Test
    void delete_deOtroUsuario_devuelve403YNoDesactiva() throws Exception {
        mvc.perform(delete("/api/users/1").with(jwt().jwt(j -> j.subject("2"))))
                .andExpect(status().isForbidden());
        verify(service, never()).delete(any());
    }

    @Test
    void listar_sinToken_devuelve401() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }
}
