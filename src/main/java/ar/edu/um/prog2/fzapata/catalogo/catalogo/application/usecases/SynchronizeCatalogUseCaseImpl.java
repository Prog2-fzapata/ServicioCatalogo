package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogEventUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.RecordSyncFailureUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SynchronizeCatalogUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraApiClient;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProcessedEventRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;

// Sin @Transactional: el pedido HTTP (hasta 10 s) no debe retener una conexión a la base
@Component
@RequiredArgsConstructor
public class SynchronizeCatalogUseCaseImpl implements SynchronizeCatalogUseCase {

    private final CatedraApiClient catedraApiClient;
    private final ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;
    private final ApplyCatalogEventUseCase applyCatalogEventUseCase;
    private final RecordSyncFailureUseCase recordSyncFailureUseCase;
    private final ProcessedEventRepository processedEventRepository;
    private final SyncStateRepository syncStateRepository;

    // Una sincronización a la vez, pedido HTTP incluido: la que espera pide después un snapshot ya fresco
    // y nunca aplica uno viejo encima de uno nuevo
    private final ReentrantLock lock = new ReentrantLock();

    private <T> T withLock(Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private void recordFailure(SyncType type, RuntimeException original) {
        try {
            recordSyncFailureUseCase.recordFailure(type, original);
        } catch (RuntimeException recordingFailure) {
            // No tapa el error original: viaja con él para que quien llama lo registre
            original.addSuppressed(recordingFailure);
        }
    }

    private <T> T fetchAndApply(Function<CatalogSnapshot, T> apply) {
        try {
            return apply.apply(catedraApiClient.fetchSnapshot());
        } catch (RuntimeException failure) {
            recordFailure(SyncType.FULL, failure);
            throw failure;
        }
    }

    // Si la copia ya tiene esa versión (o una mayor) el evento no trae nada nuevo: se evita el pedido HTTP
    private boolean hasNothingToApply(CatalogUpdatedEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return true;
        }
        SyncState state = syncStateRepository.find()
                .orElseThrow(() -> new IllegalStateException("Falta la fila id = 1 de sync_state"));
        return state.getLocalVersion() >= event.newVersion();
    }

    @Override
    public SyncResult synchronizeFull() {
        return withLock(() -> fetchAndApply(applyCatalogSnapshotUseCase::applySnapshot));
    }

    @Override
    public Optional<SyncResult> synchronizeForEvent(CatalogUpdatedEvent event) {
        return withLock(() -> hasNothingToApply(event)
                ? Optional.empty()
                : fetchAndApply(snapshot -> applyCatalogEventUseCase.applyEvent(event, snapshot)));
    }
}
