package ar.edu.um.prog2.fzapata.catalogo.usuario.application.usecases;

import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.LoginAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.RegisterUserUseCase;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.PasswordHasher;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    private static final String DEFAULT_LANG_KEY = "es";
    private static final String ROLE_USER = "ROLE_USER";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Override
    public User registerUser(User user, String rawPassword) {
        // Login y email son únicos sin distinguir mayúsculas: se guardan en minúsculas
        String login = user.getLogin().toLowerCase(Locale.ROOT);
        String email = user.getEmail().toLowerCase(Locale.ROOT);

        // Se verifica antes de hashear: el hash es costoso a propósito.
        // Un login tampoco puede ser el email de otro usuario, ni un email el login de otro: así quien
        // escribe ese texto al ingresar nunca queda ambiguo (nadie puede apropiarse del email ajeno)
        if (userRepository.existsByLogin(login) || userRepository.existsByEmail(login)) {
            throw new LoginAlreadyUsedException();
        }
        if (userRepository.existsByEmail(email) || userRepository.existsByLogin(email)) {
            throw new EmailAlreadyUsedException();
        }

        // Usuario nuevo: id, activación y roles los decide el servidor, nunca el cliente
        User newUser = User.builder()
                .login(login)
                .passwordHash(passwordHasher.hash(rawPassword))
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(email)
                .imageUrl(user.getImageUrl())
                .langKey(hasText(user.getLangKey()) ? user.getLangKey() : DEFAULT_LANG_KEY)
                .activated(true)
                .authorities(Set.of(ROLE_USER))
                .build();
        return userRepository.create(newUser);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
