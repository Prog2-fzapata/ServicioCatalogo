package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.SyncStateEntity;
import jakarta.persistence.LockModeType;

@Repository
public interface JpaSyncStateRepository extends JpaRepository<SyncStateEntity, Integer> {

    // SELECT ... FOR UPDATE: otra transacción que pida la fila espera hasta que ésta termine
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SyncStateEntity> findWithLockById(Integer id);
}
