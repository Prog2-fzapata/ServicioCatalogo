package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.adapter;

import java.util.Optional;

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
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id).map(userMapper::toDomainModel);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return jpaUserRepository.findByLogin(login).map(userMapper::toDomainModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email).map(userMapper::toDomainModel);
    }

    @Override
    public Optional<User> update(Long id, User user) {
        if (!jpaUserRepository.existsById(id)) {
            return Optional.empty();
        }
        // El id es el verificado arriba, no el que traiga el objeto
        user.setId(id);
        UserEntity saved = jpaUserRepository.save(userMapper.toEntity(user));
        return Optional.of(userMapper.toDomainModel(saved));
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
