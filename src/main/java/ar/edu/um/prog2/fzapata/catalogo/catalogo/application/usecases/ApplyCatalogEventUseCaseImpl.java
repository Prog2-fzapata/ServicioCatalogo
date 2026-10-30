package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogIncrement;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProcessedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogEventUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogIncrementUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApplyCatalogEventUseCaseImpl implements ApplyCatalogEventUseCase {

    private final ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;
    private final ApplyCatalogIncrementUseCase applyCatalogIncrementUseCase;
    private final ProcessedEventRepository processedEventRepository;
    private final Clock clock;

    private Optional<SyncResult> applyOnce(CatalogUpdatedEvent event, Supplier<SyncResult> apply) {
        if (processedEventRepository.existsById(event.eventId())) {
            return Optional.empty();
        }
        SyncResult result = apply.get();
        processedEventRepository.save(ProcessedEvent.builder()
                .eventId(event.eventId())
                .eventType(event.eventType())
                .processedAt(clock.instant())
                .build());
        return Optional.of(result);
    }

    // Datos, versión y eventId en una sola transacción: si algo falla no queda ninguno de los tres y Kafka
    // reentrega el evento. La clave primaria de processed_events rechaza al repetido si dos corren a la vez
    @Override
    @Transactional
    public Optional<SyncResult> applyEvent(CatalogUpdatedEvent event, CatalogSnapshot snapshot) {
        return applyOnce(event, () -> applyCatalogSnapshotUseCase.applySnapshot(snapshot));
    }

    @Override
    @Transactional
    public Optional<SyncResult> applyEvent(CatalogUpdatedEvent event, CatalogIncrement increment) {
        return applyOnce(event, () -> applyCatalogIncrementUseCase.applyIncrement(increment));
    }
}
