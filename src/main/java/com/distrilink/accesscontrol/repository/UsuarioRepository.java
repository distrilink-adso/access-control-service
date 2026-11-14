package com.distrilink.accesscontrol.repository;

import com.distrilink.accesscontrol.model.Usuario;
import com.distrilink.accesscontrol.model.UsuarioEstado;
import com.distrilink.accesscontrol.model.UsuarioRol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Capa de Repositories: Spring Data genera el SQL a partir del nombre del método. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    /** RF1.2: correo único al registrar. */
    boolean existsByCorreo(String correo);

    /** RF1.2 al actualizar: el correo puede repetirse solo con el mismo usuario. */
    boolean existsByCorreoAndIdNot(String correo, Long id);

    List<Usuario> findByRol(UsuarioRol rol);

    List<Usuario> findByEstado(UsuarioEstado estado);

    List<Usuario> findByRolAndEstado(UsuarioRol rol, UsuarioEstado estado);
}
