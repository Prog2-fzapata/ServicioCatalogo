package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogIncrement;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogIncrementUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatalogRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApplyCatalogIncrementUseCaseImpl implements ApplyCatalogIncrementUseCase {

    private final CatalogRepository catalogRepository;
    private final SyncStateRepository syncStateRepository;
    private final Clock clock;

    // Datos y versión en una sola transacción: si algo falla no cambia nada
    @Override
    @Transactional
    public SyncResult applyIncrement(CatalogIncrement increment) {
        SyncState state = syncStateRepository.findForUpdate()
                .orElseThrow(SyncStateMissingException::new);
        long previousVersion = state.getLocalVersion();
        // Con la fila bloqueada, esta comprobación vale aunque otra sincronización haya avanzado la versión
        if (increment.getVersion() != previousVersion + 1) {
            throw new IllegalStateException("Se esperaba la versión " + (previousVersion + 1)
                    + " y llegó la " + increment.getVersion());
        }

        catalogRepository.upsert(increment);

        state.setLocalVersion(increment.getVersion());
        state.setLastSyncAt(clock.instant());
        state.setLastSyncType(SyncType.INCREMENTAL);
        state.setLastStatus(SyncStatus.OK);
        state.setLastError(null);
        syncStateRepository.save(state);
        return new SyncResult(previousVersion, increment.getVersion(), SyncType.INCREMENTAL);
    }
}
