package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogIncrement;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.CatalogRepository;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;

@ExtendWith(MockitoExtension.class)
class ApplyCatalogIncrementUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-10-26T15:00:00Z");

    @Mock
    private CatalogRepository catalogRepository;

    @Mock
    private SyncStateRepository syncStateRepository;

    private ApplyCatalogIncrementUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ApplyCatalogIncrementUseCaseImpl(catalogRepository, syncStateRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private CatalogIncrement incrementOfVersion(long version) {
        return CatalogIncrement.builder().version(version).categories(List.of()).professionals(List.of())
                .weeklySchedules(List.of()).build();
    }

    private SyncState givenState(long localVersion) {
        SyncState state = SyncState.builder().localVersion(localVersion).lastStatus(SyncStatus.ERROR)
                .lastError("falló antes").build();
        when(syncStateRepository.findForUpdate()).thenReturn(Optional.of(state));
        return state;
    }

    @Test
    void appliesTheNextVersionAndAdvancesTheLocalOne() {
        givenState(4);
        CatalogIncrement increment = incrementOfVersion(5);

        SyncResult result = useCase.applyIncrement(increment);

        assertThat(result).isEqualTo(new SyncResult(4, 5, SyncType.INCREMENTAL));
        InOrder order = inOrder(catalogRepository, syncStateRepository);
        order.verify(catalogRepository).upsert(increment);
        ArgumentCaptor<SyncState> saved = ArgumentCaptor.forClass(SyncState.class);
        order.verify(syncStateRepository).save(saved.capture());
        assertThat(saved.getValue().getLocalVersion()).isEqualTo(5);
        assertThat(saved.getValue().getLastSyncAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getLastSyncType()).isEqualTo(SyncType.INCREMENTAL);
        assertThat(saved.getValue().getLastStatus()).isEqualTo(SyncStatus.OK);
        assertThat(saved.getValue().getLastError()).isNull();
    }

    @Test
    void rejectsAVersionThatSkipsOneWithoutTouchingTheData() {
        givenState(4);

        assertThatThrownBy(() -> useCase.applyIncrement(incrementOfVersion(6)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("5").hasMessageContaining("6");

        verify(catalogRepository, never()).upsert(any());
        verify(syncStateRepository, never()).save(any());
    }

    @Test
    void rejectsAVersionThatWasAlreadyApplied() {
        givenState(5);

        assertThatThrownBy(() -> useCase.applyIncrement(incrementOfVersion(5)))
                .isInstanceOf(IllegalStateException.class);

        verify(catalogRepository, never()).upsert(any());
    }

    @Test
    void doesNotAdvanceTheVersionWhenWritingTheDataFails() {
        givenState(4);
        CatalogIncrement increment = incrementOfVersion(5);
        doThrow(new IllegalStateException("violación de clave foránea")).when(catalogRepository).upsert(increment);

        assertThatThrownBy(() -> useCase.applyIncrement(increment)).isInstanceOf(IllegalStateException.class);

        verify(syncStateRepository, never()).save(any());
    }

    @Test
    void failsWhenTheSyncStateRowIsMissing() {
        when(syncStateRepository.findForUpdate()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.applyIncrement(incrementOfVersion(1)))
                .isInstanceOf(SyncStateMissingException.class)
                .hasMessage("La tabla sync_state está vacía, la base fue alterada");
    }
}
