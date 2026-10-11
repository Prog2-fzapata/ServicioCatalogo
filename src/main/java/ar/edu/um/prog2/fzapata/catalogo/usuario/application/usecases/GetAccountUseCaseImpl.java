package ar.edu.um.prog2.fzapata.catalogo.usuario.application.usecases;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.in.GetAccountUseCase;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GetAccountUseCaseImpl implements GetAccountUseCase {

    private final UserRepository userRepository;

    @Override
    public Optional<User> getAccount(Long userId) {
        return userRepository.findById(userId);
    }
}
