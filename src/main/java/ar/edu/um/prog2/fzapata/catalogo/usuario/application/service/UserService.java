package ar.edu.um.prog2.fzapata.catalogo.usuario.application.service;

import org.springframework.stereotype.Service;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RegisterUserUseCase registerUserUseCase;

    public User registerUser(User user, String rawPassword) {
        return registerUserUseCase.registerUser(user, rawPassword);
    }
}
