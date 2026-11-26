package com.distrilink.accesscontrol.service;

import com.distrilink.accesscontrol.dto.UsuarioRequest;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.dto.UsuarioUpdateRequest;
import com.distrilink.accesscontrol.exception.EmailAlreadyRegisteredException;
import com.distrilink.accesscontrol.exception.UsuarioNotFoundException;
import com.distrilink.accesscontrol.model.Usuario;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import com.distrilink.accesscontrol.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Prueba unitaria del Service: el repositorio y el cifrador son mocks, no se usa base de datos. */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService service;

    private Usuario saved(Long id, UsuarioRol rol, UsuarioEstado estado) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNombre("Tienda La Esquina");
        u.setCorreo("tienda@correo.com");
        u.setContrasena("$2a$10$hashViejo");
        u.setRol(rol);
        u.setEstado(estado);
        u.setFechaCreacion(LocalDateTime.now());
        u.setFechaActualizacion(LocalDateTime.now());
        return u;
    }

    @Test
    void create_guardaActivoConContrasenaCifradaYCorreoEnMinusculas() {
        when(passwordEncoder.encode("Clave1234")).thenReturn("$2a$10$hashNuevo");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        UsuarioRequest request = new UsuarioRequest("  Tienda La Esquina  ", "  TIENDA@Correo.com ",
                "Clave1234", UsuarioRol.CUSTOMER);

        UsuarioResponse response = service.create(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getContrasena()).isEqualTo("$2a$10$hashNuevo").isNotEqualTo("Clave1234");
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(UsuarioEstado.ACTIVE);
        assertThat(response.email()).isEqualTo("tienda@correo.com");
        assertThat(response.name()).isEqualTo("Tienda La Esquina");
    }

    @Test
    void create_correoDuplicadoLanzaExcepcionYNoGuarda() {
        when(repository.existsByCorreo("tienda@correo.com")).thenReturn(true);
        UsuarioRequest request = new UsuarioRequest("Tienda", "TIENDA@correo.com", "Clave1234", UsuarioRol.CUSTOMER);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("tienda@correo.com");
        verify(repository, never()).save(any());
    }

    @Test
    void findAll_conRolFiltraPorRol() {
        when(repository.findByRol(UsuarioRol.CUSTOMER))
                .thenReturn(List.of(saved(1L, UsuarioRol.CUSTOMER, UsuarioEstado.ACTIVE)));

        List<UsuarioResponse> result = service.findAll(UsuarioRol.CUSTOMER, null);

        assertThat(result).hasSize(1);
        verify(repository, never()).findAll();
    }

    @Test
    void findAll_sinFiltrosDevuelveTodos() {
        when(repository.findAll()).thenReturn(List.of(
                saved(1L, UsuarioRol.CUSTOMER, UsuarioEstado.ACTIVE),
                saved(2L, UsuarioRol.DISTRIBUTOR, UsuarioEstado.INACTIVE)));

        assertThat(service.findAll(null, null)).hasSize(2);
    }

    @Test
    void findById_inexistenteLanzaExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(UsuarioNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void update_sinContrasenaConservaLaActual() {
        when(repository.findById(1L)).thenReturn(Optional.of(saved(1L, UsuarioRol.CUSTOMER, UsuarioEstado.ACTIVE)));
        when(repository.existsByCorreoAndIdNot("nuevo@correo.com", 1L)).thenReturn(false);
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        UsuarioUpdateRequest request = new UsuarioUpdateRequest("Nuevo Nombre", "NUEVO@correo.com", null,
                UsuarioRol.DISTRIBUTOR, UsuarioEstado.LOCKED);

        UsuarioResponse response = service.update(1L, request);

        assertThat(response.name()).isEqualTo("Nuevo Nombre");
        assertThat(response.email()).isEqualTo("nuevo@correo.com");
        assertThat(response.role()).isEqualTo(UsuarioRol.DISTRIBUTOR);
        assertThat(response.status()).isEqualTo(UsuarioEstado.LOCKED);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void update_conContrasenaLaCifraDeNuevo() {
        Usuario existente = saved(1L, UsuarioRol.CUSTOMER, UsuarioEstado.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.existsByCorreoAndIdNot("tienda@correo.com", 1L)).thenReturn(false);
        when(passwordEncoder.encode("OtraClave99")).thenReturn("$2a$10$hashOtro");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        UsuarioUpdateRequest request = new UsuarioUpdateRequest("Tienda La Esquina", "tienda@correo.com",
                "OtraClave99", UsuarioRol.CUSTOMER, UsuarioEstado.ACTIVE);

        service.update(1L, request);

        assertThat(existente.getContrasena()).isEqualTo("$2a$10$hashOtro");
    }

    @Test
    void update_correoDeOtroUsuarioLanzaExcepcionYNoGuarda() {
        when(repository.findById(1L)).thenReturn(Optional.of(saved(1L, UsuarioRol.CUSTOMER, UsuarioEstado.ACTIVE)));
        when(repository.existsByCorreoAndIdNot("ocupado@correo.com", 1L)).thenReturn(true);
        UsuarioUpdateRequest request = new UsuarioUpdateRequest("Tienda", "ocupado@correo.com", null,
                UsuarioRol.CUSTOMER, UsuarioEstado.ACTIVE);

        assertThatThrownBy(() -> service.update(1L, request)).isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void delete_desactivaLaCuentaSinBorrarElRegistro() {
        Usuario existente = saved(1L, UsuarioRol.CUSTOMER, UsuarioEstado.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(1L);

        assertThat(existente.getEstado()).isEqualTo(UsuarioEstado.INACTIVE);
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_inexistenteLanzaExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(UsuarioNotFoundException.class);
        verify(repository, never()).save(any());
    }
}
