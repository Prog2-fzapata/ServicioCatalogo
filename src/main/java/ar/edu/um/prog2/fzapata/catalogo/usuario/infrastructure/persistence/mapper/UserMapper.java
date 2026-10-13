package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.mapper;

import java.util.HashSet;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.entity.UserEntity;

@Component
public class UserMapper {

    public UserEntity toEntity(User user) {
        if (user == null) return null;
        return UserEntity.builder()
                .id(user.getId())
                .login(user.getLogin())
                .passwordHash(user.getPasswordHash())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .imageUrl(user.getImageUrl())
                .langKey(user.getLangKey())
                .activated(user.isActivated())
                // Se copia el Set para que dominio y entidad no compartan la misma colección
                .authorities(user.getAuthorities() == null ? new HashSet<>() : new HashSet<>(user.getAuthorities()))
                .createdDate(user.getCreatedDate())
                .lastModifiedDate(user.getLastModifiedDate())
                .build();
    }

    public User toDomainModel(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .login(entity.getLogin())
                .passwordHash(entity.getPasswordHash())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .imageUrl(entity.getImageUrl())
                .langKey(entity.getLangKey())
                .activated(entity.isActivated())
                .authorities(entity.getAuthorities() == null ? new HashSet<>() : new HashSet<>(entity.getAuthorities()))
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}
