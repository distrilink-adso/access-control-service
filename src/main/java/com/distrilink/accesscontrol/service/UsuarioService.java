package com.distrilink.accesscontrol.service;

import com.distrilink.accesscontrol.dto.UsuarioRequest;
import com.distrilink.accesscontrol.dto.UsuarioResponse;
import com.distrilink.accesscontrol.dto.UsuarioUpdateRequest;
import com.distrilink.accesscontrol.exception.EmailAlreadyRegisteredException;
import com.distrilink.accesscontrol.exception.InvalidPasswordException;
import com.distrilink.accesscontrol.exception.UsuarioNotFoundException;
import com.distrilink.accesscontrol.model.Usuario;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import com.distrilink.accesscontrol.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/** Capa de Services: aquí viven las reglas de negocio, no en el Controller. */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    /** BCrypt solo procesa los primeros 72 bytes de la contraseña. */
    private static final int MAX_PASSWORD_BYTES = 72;

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    /** RF1.1 + RF1.2 + RF1.3: registra un usuario ACTIVO, con correo único y contraseña cifrada. */
    @Transactional
    public UsuarioResponse create(UsuarioRequest request) {
        String correo = normalizar(request.correo());
        if (repository.existsByCorreo(correo)) {
            throw new EmailAlreadyRegisteredException(correo);
        }
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setContrasena(cifrar(request.contrasena()));
        usuario.setRol(request.rol());
        usuario.setEstado(UsuarioEstado.ACTIVO);
        return UsuarioResponse.from(repository.save(usuario));
    }

    /** Lista todos los usuarios; los filtros por rol y estado son opcionales. */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> findAll(UsuarioRol rol, UsuarioEstado estado) {
        List<Usuario> result;
        if (rol != null && estado != null) {
            result = repository.findByRolAndEstado(rol, estado);
        } else if (rol != null) {
            result = repository.findByRol(rol);
        } else if (estado != null) {
            result = repository.findByEstado(estado);
        } else {
            result = repository.findAll();
        }
        return result.stream().map(UsuarioResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse findById(Long id) {
        return UsuarioResponse.from(getOrThrow(id));
    }

    /** RF1.11: actualiza los datos. La contraseña solo se cifra y reemplaza si viene en la petición. */
    @Transactional
    public UsuarioResponse update(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = getOrThrow(id);
        String correo = normalizar(request.correo());
        if (repository.existsByCorreoAndIdNot(correo, id)) {
            throw new EmailAlreadyRegisteredException(correo);
        }
        usuario.setNombre(request.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setRol(request.rol());
        usuario.setEstado(request.estado());
        if (request.contrasena() != null) {
            usuario.setContrasena(cifrar(request.contrasena()));
        }
        return UsuarioResponse.from(repository.save(usuario));
    }

    /**
     * RF1.12: "eliminar" desactiva la cuenta, NO borra el registro. Los pedidos y las notificaciones
     * guardan el id del usuario en otras bases de datos, así que el registro debe conservarse.
     */
    @Transactional
    public void delete(Long id) {
        Usuario usuario = getOrThrow(id);
        usuario.setEstado(UsuarioEstado.INACTIVO);
        repository.save(usuario);
    }

    private Usuario getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new UsuarioNotFoundException(id));
    }

    private String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }

    private String cifrar(String contrasena) {
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new InvalidPasswordException("La contraseña es demasiado larga (máximo 72 bytes)");
        }
        return passwordEncoder.encode(contrasena);
    }
}
