package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogIncrement;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.ProcessedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogIncrementUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.ApplyCatalogSnapshotUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.ProcessedEventRepository;

@ExtendWith(MockitoExtension.class)
class ApplyCatalogEventUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-10-25T15:00:00Z");
    private static final CatalogUpdatedEvent EVENT = new CatalogUpdatedEvent("evento-7", "CatalogUpdated", 7);
    private static final CatalogSnapshot SNAPSHOT = CatalogSnapshot.builder().version(7).build();
    private static final CatalogIncrement INCREMENT = CatalogIncrement.builder().version(7).build();

    @Mock
    private ApplyCatalogSnapshotUseCase applyCatalogSnapshotUseCase;

    @Mock
    private ApplyCatalogIncrementUseCase applyCatalogIncrementUseCase;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private ApplyCatalogEventUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApplyCatalogEventUseCaseImpl(applyCatalogSnapshotUseCase, applyCatalogIncrementUseCase,
                processedEventRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void appliesTheSnapshotAndThenRecordsTheEvent() {
        when(applyCatalogSnapshotUseCase.applySnapshot(SNAPSHOT)).thenReturn(new SyncResult(3, 7, SyncType.FULL));

        assertThat(useCase.applyEvent(EVENT, SNAPSHOT)).contains(new SyncResult(3, 7, SyncType.FULL));

        // Primero los datos y la versión; el eventId se guarda al final, en la misma transacción
        InOrder order = Mockito.inOrder(applyCatalogSnapshotUseCase, processedEventRepository);
        order.verify(applyCatalogSnapshotUseCase).applySnapshot(SNAPSHOT);
        ArgumentCaptor<ProcessedEvent> saved = ArgumentCaptor.forClass(ProcessedEvent.class);
        order.verify(processedEventRepository).save(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo("evento-7");
        assertThat(saved.getValue().getEventType()).isEqualTo("CatalogUpdated");
        assertThat(saved.getValue().getProcessedAt()).isEqualTo(NOW);
    }

    @Test
    void changesNothingWhenTheEventWasAlreadyProcessed() {
        when(processedEventRepository.existsById("evento-7")).thenReturn(true);

        assertThat(useCase.applyEvent(EVENT, SNAPSHOT)).isEmpty();

        verify(applyCatalogSnapshotUseCase, never()).applySnapshot(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void doesNotRecordTheEventWhenApplyingTheSnapshotFails() {
        when(applyCatalogSnapshotUseCase.applySnapshot(SNAPSHOT)).thenThrow(new IllegalStateException("falló"));

        assertThatThrownBy(() -> useCase.applyEvent(EVENT, SNAPSHOT)).isInstanceOf(IllegalStateException.class);

        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void appliesTheIncrementAndThenRecordsTheEvent() {
        when(applyCatalogIncrementUseCase.applyIncrement(INCREMENT)).thenReturn(new SyncResult(6, 7, SyncType.INCREMENTAL));

        assertThat(useCase.applyEvent(EVENT, INCREMENT)).contains(new SyncResult(6, 7, SyncType.INCREMENTAL));

        InOrder order = Mockito.inOrder(applyCatalogIncrementUseCase, processedEventRepository);
        order.verify(applyCatalogIncrementUseCase).applyIncrement(INCREMENT);
        ArgumentCaptor<ProcessedEvent> saved = ArgumentCaptor.forClass(ProcessedEvent.class);
        order.verify(processedEventRepository).save(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo("evento-7");
        assertThat(saved.getValue().getProcessedAt()).isEqualTo(NOW);
        verify(applyCatalogSnapshotUseCase, never()).applySnapshot(any());
    }

    @Test
    void changesNothingWhenTheEventWasAlreadyProcessedBeforeApplyingTheIncrement() {
        when(processedEventRepository.existsById("evento-7")).thenReturn(true);

        assertThat(useCase.applyEvent(EVENT, INCREMENT)).isEmpty();

        verify(applyCatalogIncrementUseCase, never()).applyIncrement(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void doesNotRecordTheEventWhenApplyingTheIncrementFails() {
        when(applyCatalogIncrementUseCase.applyIncrement(INCREMENT)).thenThrow(new IllegalStateException("falló"));

        assertThatThrownBy(() -> useCase.applyEvent(EVENT, INCREMENT)).isInstanceOf(IllegalStateException.class);

        verify(processedEventRepository, never()).save(any());
    }
}
