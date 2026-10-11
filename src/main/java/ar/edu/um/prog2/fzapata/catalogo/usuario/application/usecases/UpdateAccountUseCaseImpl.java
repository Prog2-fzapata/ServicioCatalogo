package ar.edu.um.prog2.fzapata.catalogo.usuario.application.usecases;

import java.util.Locale;
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
        Optional<User> current = userRepository.findById(userId);
        if (current.isEmpty()) {
            return Optional.empty();
        }
        User user = current.get();

        String email = changes.getEmail().toLowerCase(Locale.ROOT);
        // Conservar el propio email es válido; no puede ser el email ni el login de otro usuario
        if (isAnotherUser(userRepository.findByEmail(email), userId)
                || isAnotherUser(userRepository.findByLogin(email), userId)) {
            throw new EmailAlreadyUsedException();
        }

        // Solo estos campos se pueden cambiar acá: login, contraseña, roles e id quedan como están
        user.setFirstName(changes.getFirstName());
        user.setLastName(changes.getLastName());
        user.setEmail(email);
        user.setImageUrl(changes.getImageUrl());
        if (changes.getLangKey() != null && !changes.getLangKey().isBlank()) {
            user.setLangKey(changes.getLangKey());
        }
        return userRepository.update(userId, user);
    }

    private boolean isAnotherUser(Optional<User> found, Long userId) {
        return found.filter(other -> !other.getId().equals(userId)).isPresent();
    }
}
