package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.SyncStateEntity;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.SyncStateMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaSyncStateRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JpaSyncStateRepositoryAdapter implements SyncStateRepository {

    private final JpaSyncStateRepository jpaSyncStateRepository;
    private final SyncStateMapper syncStateMapper;

    @Override
    public Optional<SyncState> findForUpdate() {
        return jpaSyncStateRepository.findWithLockById(SyncStateEntity.SINGLE_ROW_ID)
                .map(syncStateMapper::toDomainModel);
    }

    @Override
    public void save(SyncState state) {
        jpaSyncStateRepository.save(syncStateMapper.toEntity(state));
    }
}
