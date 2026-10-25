package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.RecordSyncFailureUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SynchronizeCatalogUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraApiClient;
import lombok.RequiredArgsConstructor;

// Sin @Transactional: el pedido HTTP (hasta 10 s) no debe retener una conexión a la base
@Component
@RequiredArgsConstructor
public class SynchronizeCatalogUseCaseImpl implements SynchronizeCatalogUseCase {

    private final CatedraApiClient catedraApiClient;
    private final ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;
    private final RecordSyncFailureUseCase recordSyncFailureUseCase;

    // Una sincronización a la vez, pedido HTTP incluido: la que espera pide después un snapshot ya fresco
    // y nunca aplica uno viejo encima de uno nuevo
    private final ReentrantLock lock = new ReentrantLock();

    private void recordFailure(SyncType type, RuntimeException original) {
        try {
            recordSyncFailureUseCase.recordFailure(type, original);
        } catch (RuntimeException recordingFailure) {
            // No tapa el error original: viaja con él para que quien llama lo registre
            original.addSuppressed(recordingFailure);
        }
    }

    private SyncResult fetchAndApply() {
        try {
            CatalogSnapshot snapshot = catedraApiClient.fetchSnapshot();
            return applyCatalogSnapshotUseCase.applySnapshot(snapshot);
        } catch (RuntimeException failure) {
            recordFailure(SyncType.FULL, failure);
            throw failure;
        }
    }

    @Override
    public SyncResult synchronizeFull() {
        lock.lock();
        try {
            return fetchAndApply();
        } finally {
            lock.unlock();
        }
    }
}
