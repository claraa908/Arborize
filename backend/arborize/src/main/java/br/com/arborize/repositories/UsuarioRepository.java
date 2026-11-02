package br.com.arborize.repositories;

import br.com.arborize.entities.Usuario;
import br.com.arborize.enums.UsuarioRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByCpf(String cpf);
    boolean existsByEmailIgnoreCase(String email);
    Optional<Usuario> findByCpf(String cpf);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    List<Usuario> findByRoleInOrderByNomeAsc(Collection<br.com.arborize.enums.UsuarioRole> roles);
}
