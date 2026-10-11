package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto.AccountResponse;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.web.dto.AccountUpdateRequest;
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

    // Solo los campos editables; login, contraseña y roles no se pueden cambiar por /api/account
    public User toDomain(AccountUpdateRequest request) {
        if (request == null) return null;
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .langKey(request.getLangKey())
                .build();
    }

    public AccountResponse toResponse(User user) {
        if (user == null) return null;
        return AccountResponse.builder()
                .id(user.getId())
                .login(user.getLogin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .langKey(user.getLangKey())
                .authorities(user.getAuthorities())
                .build();
    }
}
