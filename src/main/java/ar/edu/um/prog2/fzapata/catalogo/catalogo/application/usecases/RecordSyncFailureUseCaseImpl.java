package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.RecordSyncFailureUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecordSyncFailureUseCaseImpl implements RecordSyncFailureUseCase {

    // El mensaje de una excepción de la cátedra puede ser largo; el estado solo necesita la causa
    private static final int MAX_ERROR_LENGTH = 500;

    private final SyncStateRepository syncStateRepository;
    private final Clock clock;

    // Se guarda el mensaje solo de nuestras propias excepciones de la cátedra, que escribimos nosotros: el de
    // Hibernate, el driver o Redis puede traer la sentencia SQL, nombres de columnas o valores. De cualquier otra se
    // guarda solo la clase; el detalle completo queda en el log. Así lo que expone GET /api/sync/status es seguro
    private String describe(Throwable cause) {
        String name = cause.getClass().getSimpleName();
        String text = cause instanceof CatedraException && cause.getMessage() != null
                ? name + ": " + cause.getMessage()
                : name;
        return text.length() <= MAX_ERROR_LENGTH ? text : text.substring(0, MAX_ERROR_LENGTH);
    }

    // Transacción propia: la de los datos ya se deshizo y arrastraría este registro con ella.
    // No cambia localVersion: la copia quedó como estaba
    @Override
    @Transactional
    public void recordFailure(SyncType type, Throwable cause) {
        SyncState state = syncStateRepository.findForUpdate()
                .orElseThrow(SyncStateMissingException::new);
        state.setLastSyncAt(clock.instant());
        state.setLastSyncType(type);
        state.setLastStatus(SyncStatus.ERROR);
        state.setLastError(describe(cause));
        syncStateRepository.save(state);
    }
}
