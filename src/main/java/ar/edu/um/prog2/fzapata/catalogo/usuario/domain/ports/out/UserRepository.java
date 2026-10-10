package ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;

public interface UserRepository {
    User create(User user);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
