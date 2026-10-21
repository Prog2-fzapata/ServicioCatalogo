package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.entity.SyncStateEntity;

@Component
public class SyncStateMapper {

    public SyncStateEntity toEntity(SyncState state) {
        if (state == null) return null;
        return SyncStateEntity.builder()
                .id(SyncStateEntity.SINGLE_ROW_ID)
                .localVersion(state.getLocalVersion())
                .lastSyncAt(state.getLastSyncAt())
                .lastSyncType(state.getLastSyncType())
                .lastStatus(state.getLastStatus())
                .lastError(state.getLastError())
                .build();
    }

    public SyncState toDomainModel(SyncStateEntity entity) {
        if (entity == null) return null;
        return SyncState.builder()
                .localVersion(entity.getLocalVersion())
                .lastSyncAt(entity.getLastSyncAt())
                .lastSyncType(entity.getLastSyncType())
                .lastStatus(entity.getLastStatus())
                .lastError(entity.getLastError())
                .build();
    }
}
