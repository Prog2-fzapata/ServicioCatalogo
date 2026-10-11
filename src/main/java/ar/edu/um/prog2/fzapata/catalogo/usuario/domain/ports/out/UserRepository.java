package ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out;

import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;

public interface UserRepository {
    User create(User user);

    Optional<User> findById(Long id);

    Optional<User> findByLogin(String login);

    Optional<User> findByEmail(String email);

    Optional<User> update(Long id, User user);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
