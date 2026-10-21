package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.RecordSyncFailureUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecordSyncFailureUseCaseImpl implements RecordSyncFailureUseCase {

    // Un mensaje de Hibernate puede traer la sentencia SQL entera; el estado solo necesita la causa
    private static final int MAX_ERROR_LENGTH = 500;

    private final SyncStateRepository syncStateRepository;
    private final Clock clock;

    // Transacción propia: la de los datos ya se deshizo y arrastraría este registro con ella.
    // No cambia localVersion: la copia quedó como estaba
    @Override
    @Transactional
    public void recordFailure(SyncType type, Throwable cause) {
        SyncState state = syncStateRepository.findForUpdate()
                .orElseThrow(() -> new IllegalStateException("Falta la fila id = 1 de sync_state"));
        state.setLastSyncAt(clock.instant());
        state.setLastSyncType(type);
        state.setLastStatus(SyncStatus.ERROR);
        state.setLastError(describe(cause));
        syncStateRepository.save(state);
    }

    private String describe(Throwable cause) {
        String text = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH);
    }
}
