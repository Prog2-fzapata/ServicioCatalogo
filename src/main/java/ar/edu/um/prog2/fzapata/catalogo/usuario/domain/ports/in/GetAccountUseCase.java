package ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in;

import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;

public interface GetAccountUseCase {
    Optional<User> getAccount(Long userId);
}
