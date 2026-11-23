package ar.edu.um.prog2.fzapata.catalogo.usuario.application.usecases;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.usuario.application.exception.EmailAlreadyUsedException;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.UpdateAccountUseCase;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UpdateAccountUseCaseImpl implements UpdateAccountUseCase {

    private final UserRepository userRepository;

    @Override
    public Optional<User> updateAccount(Long userId, User changes) {
        Objects.requireNonNull(changes, "Los cambios son obligatorios");
        Optional<User> current = userRepository.findById(userId);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        User user = current.get();

        // Solo cambia lo que llega: un campo ausente (null) conserva su valor. Login, contraseña, roles e id no se
        // cambian por acá
        if (changes.getEmail() != null) {
            String email = changes.getEmail().toLowerCase(Locale.ROOT);
            requireNotUsedByAnother(email, userId);
            user.setEmail(email);
        }
        user.setFirstName(textOrKeep(changes.getFirstName(), user.getFirstName()));
        user.setLastName(textOrKeep(changes.getLastName(), user.getLastName()));
        user.setImageUrl(textOrKeep(changes.getImageUrl(), user.getImageUrl()));
        user.setLangKey(valueOrKeep(changes.getLangKey(), user.getLangKey()));
        return userRepository.update(userId, user);
    }

    // Conservar el propio email es válido; no puede ser el email ni el login de otro usuario
    private void requireNotUsedByAnother(String email, Long userId) {
        if (isAnotherUser(userRepository.findByEmail(email), userId)
                || isAnotherUser(userRepository.findByLogin(email), userId)) {
            throw new EmailAlreadyUsedException();
        }
    }

    // Ausente conserva el valor; un texto vacío lo borra (queda como si nunca se hubiera cargado)
    private String textOrKeep(String sent, String current) {
        if (sent == null) {
            return current;
        }
        return sent.isBlank() ? null : sent;
    }

    // Ausente o en blanco conserva el valor: el dato no puede quedar vacío
    private String valueOrKeep(String sent, String current) {
        return sent == null || sent.isBlank() ? current : sent;
    }

    private boolean isAnotherUser(Optional<User> found, Long userId) {
        return found.filter(other -> !other.getId().equals(userId)).isPresent();
    }
}
