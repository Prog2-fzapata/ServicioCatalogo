package ar.edu.um.prog2.fzapata.catalogo.usuario.application.usecases;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.AuthenticateUserUseCase;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.PasswordHasher;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.TokenIssuer;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthenticateUserUseCaseImpl implements AuthenticateUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    @Override
    public Optional<String> authenticateUser(String username, String rawPassword, boolean rememberMe) {
        // Login y email se guardan en minúsculas
        String normalized = username.toLowerCase(Locale.ROOT);
        Optional<User> byLogin = userRepository.findByLogin(normalized);
        // Si es el mismo usuario, no se lo prueba dos veces (el hash es costoso)
        Optional<User> byEmail = userRepository.findByEmail(normalized)
                .filter(user -> byLogin.map(found -> !found.getId().equals(user.getId())).orElse(true));

        // Un texto puede ser el login de un usuario y el email de otro: entra el que acierte la contraseña.
        // Si acertaran las dos, gana el login (findFirst corta en el primero)
        return Stream.of(byLogin, byEmail)
                .flatMap(Optional::stream)
                .filter(User::isActivated)
                .filter(user -> passwordHasher.matches(rawPassword, user.getPasswordHash()))
                .findFirst()
                .map(user -> tokenIssuer.issueToken(user, rememberMe));
    }
}
