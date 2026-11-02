package ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;

public interface RegisterUserUseCase {
    User registerUser(User user, String rawPassword);
}
