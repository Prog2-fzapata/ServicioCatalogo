package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.model.User;
import ar.edu.um.prog2.fzapata.catalogo.usuario.domain.ports.out.UserRepository;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.entity.UserEntity;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.mapper.UserMapper;
import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final UserMapper userMapper;

    @Override
    public User create(User user) {
        UserEntity entity = userMapper.toEntity(user);
        UserEntity saved = jpaUserRepository.save(entity);
        return userMapper.toDomainModel(saved);
    }

    @Override
    public boolean existsByLogin(String login) {
        return jpaUserRepository.existsByLogin(login);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }
}
