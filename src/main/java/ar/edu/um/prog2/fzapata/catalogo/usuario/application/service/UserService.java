package ar.edu.um.prog2.fzapata.catalogo.usuario.application.service;

import org.springframework.stereotype.Service;

import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.InvalidCredentialsException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.UserNotFoundException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.AuthenticateUserUseCase;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.GetAccountUseCase;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.RegisterUserUseCase;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.UpdateAccountUseCase;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final UpdateAccountUseCase updateAccountUseCase;

    public User registerUser(User user, String rawPassword) {
        return registerUserUseCase.registerUser(user, rawPassword);
    }

    public String authenticateUser(String username, String rawPassword, boolean rememberMe) {
        return authenticateUserUseCase.authenticateUser(username, rawPassword, rememberMe)
                .orElseThrow(InvalidCredentialsException::new);
    }

    public User getAccount(Long userId) {
        return getAccountUseCase.getAccount(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    public User updateAccount(Long userId, User changes) {
        return updateAccountUseCase.updateAccount(userId, changes).orElseThrow(() -> new UserNotFoundException(userId));
    }
}
