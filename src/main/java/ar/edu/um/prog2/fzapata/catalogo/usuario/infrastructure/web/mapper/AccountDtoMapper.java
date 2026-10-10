package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto.UserRegistrationRequest;

@Component
public class AccountDtoMapper {

    // La contraseña no se copia al modelo: viaja aparte hasta el hasher
    public User toDomain(UserRegistrationRequest request) {
        if (request == null) return null;
        return User.builder()
                .login(request.getLogin())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .langKey(request.getLangKey())
                .build();
    }
}
