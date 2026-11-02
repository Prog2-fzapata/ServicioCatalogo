package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.mapper;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto.SyncStatusResponse;

@Component
public class SyncStatusDtoMapper {

    public SyncStatusResponse toResponse(SyncState state) {
        if (state == null) return null;
        return SyncStatusResponse.builder()
                .localVersion(state.getLocalVersion())
                .lastSyncAt(state.getLastSyncAt())
                .lastSyncType(state.getLastSyncType())
                .lastStatus(state.getLastStatus())
                .lastError(state.getLastError())
                .build();
    }
}
