package ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.um.prog2.fzapata.catalogo.usuario.infrastructure.persistence.entity.UserEntity;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
