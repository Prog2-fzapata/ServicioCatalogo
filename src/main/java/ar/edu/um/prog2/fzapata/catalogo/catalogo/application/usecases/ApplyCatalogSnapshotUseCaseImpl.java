package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatalogRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApplyCatalogSnapshotUseCaseImpl implements ApplyCatalogSnapshotUseCase {

    private final CatalogRepository catalogRepository;
    private final SyncStateRepository syncStateRepository;
    private final Clock clock;

    // Datos y versión en una sola transacción: si algo falla no cambia nada
    @Override
    @Transactional
    public SyncResult applySnapshot(CatalogSnapshot snapshot) {
        SyncState state = syncStateRepository.findForUpdate()
                .orElseThrow(() -> new IllegalStateException("Falta la fila id = 1 de sync_state"));
        long previousVersion = state.getLocalVersion();

        // Se aplica aunque la versión sea menor: la cátedra es la fuente de verdad (el aviso lo da quien llama)
        catalogRepository.replaceAll(snapshot);

        state.setLocalVersion(snapshot.getVersion());
        state.setLastSyncAt(clock.instant());
        state.setLastSyncType(SyncType.FULL);
        state.setLastStatus(SyncStatus.OK);
        state.setLastError(null);
        syncStateRepository.save(state);
        return new SyncResult(previousVersion, snapshot.getVersion());
    }
}
