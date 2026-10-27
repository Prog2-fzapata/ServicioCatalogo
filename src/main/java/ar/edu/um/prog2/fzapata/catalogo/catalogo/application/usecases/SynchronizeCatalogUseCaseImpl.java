package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogChanges;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogIncrement;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncMetadata;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogEventUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogIncrementUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.RecordSyncFailureUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SynchronizeCatalogUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraApiClient;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatedraSyncSource;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProcessedEventRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;

// Sin @Transactional: el pedido HTTP (hasta 10 s) no debe retener una conexión a la base
@Component
@RequiredArgsConstructor
public class SynchronizeCatalogUseCaseImpl implements SynchronizeCatalogUseCase {

    private final CatedraApiClient catedraApiClient;
    private final CatedraSyncSource catedraSyncSource;
    private final ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;
    private final ApplyCatalogIncrementUseCase applyCatalogIncrementUseCase;
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

    private long localVersion() {
        SyncState state = syncStateRepository.find()
                .orElseThrow(SyncStateMissingException::new);
        return state.getLocalVersion();
    }

    private String describe(RuntimeException failure) {
        String name = failure.getClass().getSimpleName();
        return failure.getMessage() == null ? name : name + ": " + failure.getMessage();
    }

    private CatalogIncrement readIncrement(long version) {
        CatalogChanges changes = catedraSyncSource.readChanges(version).orElseThrow(
                () -> new IllegalStateException("La cátedra ya no conserva los cambios de la versión " + version));
        return CatalogIncrement.builder()
                .version(version)
                .categories(catedraSyncSource.readCategories(changes.categoryIds()))
                .professionals(catedraSyncSource.readProfessionals(changes.professionalIds()))
                .weeklySchedules(catedraSyncSource.readWeeklySchedules(changes.weeklyScheduleIds()))
                .build();
    }

    // Si el snapshot también falla, el error de la incremental viaja con él para que quien llama lo registre.
    // Con evento, este queda procesado en la misma transacción; sin evento (verificación de versión) no hay eventId
    private Optional<SyncResult> applySnapshotFor(Optional<CatalogUpdatedEvent> event, String reason,
            RuntimeException cause) {
        try {
            return fetchAndApply(snapshot -> event
                    .map(triggering -> applyCatalogEventUseCase.applyEvent(triggering, snapshot))
                    .orElseGet(() -> Optional.of(applyCatalogSnapshotUseCase.applySnapshot(snapshot))))
                    .map(result -> result.withSnapshotReason(reason));
        } catch (RuntimeException failure) {
            if (cause != null) {
                failure.addSuppressed(cause);
            }
            throw failure;
        }
    }

    private Optional<SyncResult> applyLastIncrement(Optional<CatalogUpdatedEvent> event, CatalogIncrement increment) {
        return event
                .map(triggering -> applyCatalogEventUseCase.applyEvent(triggering, increment))
                .orElseGet(() -> Optional.of(applyCatalogIncrementUseCase.applyIncrement(increment)));
    }

    // Una transacción por versión: si la cátedra falla a mitad queda lo ya aplicado. El evento se registra en la
    // última, para que nunca figure como procesado uno que no se terminó de aplicar
    private Optional<SyncResult> applyIncrementsFor(Optional<CatalogUpdatedEvent> event, long localVersion,
            long currentVersion) {
        try {
            for (long version = localVersion + 1; version < currentVersion; version++) {
                applyCatalogIncrementUseCase.applyIncrement(readIncrement(version));
            }
            return applyLastIncrement(event, readIncrement(currentVersion))
                    .map(last -> new SyncResult(localVersion, currentVersion, SyncType.INCREMENTAL));
        } catch (RuntimeException failure) {
            // Cualquier fallo de la incremental (falta historial, dato inválido, la base lo rechaza) cae al snapshot
            return applySnapshotFor(event, "falló la incremental: " + describe(failure), failure);
        }
    }

    // Un evento ya procesado, o con justo la versión que tiene la copia, no necesita ni leer Redis. Una versión
    // local mayor no se ignora: puede ser un retroceso de la cátedra
    private boolean isAlreadyHandled(Optional<CatalogUpdatedEvent> event, long localVersion) {
        return event.isPresent() && (processedEventRepository.existsById(event.get().eventId())
                || localVersion == event.get().newVersion());
    }

    // La versión que importa es la actual de Redis, no la del evento: el evento (o el reloj de la verificación
    // periódica) solo dispara. Un evento viejo que llega tarde no hace nada, y una versión local mayor es un
    // retroceso de la cátedra, que es la fuente de verdad (D60)
    private Optional<SyncResult> synchronizeFor(Optional<CatalogUpdatedEvent> event) {
        long localVersion = localVersion();
        if (isAlreadyHandled(event, localVersion)) {
            return Optional.empty();
        }
        SyncMetadata metadata;
        try {
            metadata = catedraSyncSource.readMetadata();
        } catch (RuntimeException failure) {
            return applySnapshotFor(event, "no se pudo leer Redis: " + describe(failure), failure);
        }
        if (localVersion == metadata.currentVersion()) {
            return Optional.empty();
        }
        if (localVersion == 0) {
            return applySnapshotFor(event, "la base está vacía", null);
        }
        if (localVersion > metadata.currentVersion()) {
            return applySnapshotFor(event, "la cátedra retrocedió de la versión " + localVersion + " a la "
                    + metadata.currentVersion(), null);
        }
        if (localVersion < metadata.oldestAvailableVersion()) {
            return applySnapshotFor(event, "la versión local " + localVersion + " es anterior a la más antigua "
                    + "disponible (" + metadata.oldestAvailableVersion() + ")", null);
        }
        return applyIncrementsFor(event, localVersion, metadata.currentVersion());
    }

    @Override
    public SyncResult synchronizeFull() {
        return withLock(() -> fetchAndApply(applyCatalogSnapshotUseCase::applySnapshot));
    }

    @Override
    public Optional<SyncResult> synchronize() {
        return withLock(() -> synchronizeFor(Optional.empty()));
    }

    @Override
    public Optional<SyncResult> synchronizeForEvent(CatalogUpdatedEvent event) {
        return withLock(() -> synchronizeFor(Optional.of(event)));
    }
}
