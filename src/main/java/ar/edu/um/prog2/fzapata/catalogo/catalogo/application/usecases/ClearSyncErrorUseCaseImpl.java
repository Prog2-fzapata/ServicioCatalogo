package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ClearSyncErrorUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClearSyncErrorUseCaseImpl implements ClearSyncErrorUseCase {

    private final SyncStateRepository syncStateRepository;
    private final Clock clock;

    // Solo escribe si había un error que limpiar: una verificación al día no toca la base cada cinco minutos.
    // No cambia localVersion ni el tipo del último intento: la copia sigue siendo la que era
    @Override
    @Transactional
    public void clearError() {
        SyncState state = syncStateRepository.findForUpdate().orElseThrow(SyncStateMissingException::new);
        if (state.getLastStatus() != SyncStatus.ERROR) {
            return;
        }
        state.setLastSyncAt(clock.instant());
        state.setLastStatus(SyncStatus.OK);
        state.setLastError(null);
        syncStateRepository.save(state);
    }
}
