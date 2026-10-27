package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.SyncStateMissingException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.GetSyncStatusUseCase;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in.SynchronizeCatalogUseCase;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private SynchronizeCatalogUseCase synchronizeCatalogUseCase;

    @Mock
    private GetSyncStatusUseCase getSyncStatusUseCase;

    @InjectMocks
    private CatalogService service;

    @Test
    void returnsTheSyncStatus() {
        SyncState state = SyncState.builder().localVersion(5).build();
        when(getSyncStatusUseCase.getSyncStatus()).thenReturn(Optional.of(state));

        assertThat(service.getSyncStatus()).isSameAs(state);
    }

    // Una base sin sincronizar tiene la fila con versión 0; sin fila, la base fue alterada
    @Test
    void failsWhenTheSyncStateRowIsMissing() {
        when(getSyncStatusUseCase.getSyncStatus()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSyncStatus()).isInstanceOf(SyncStateMissingException.class)
                .hasMessage("La tabla sync_state está vacía, la base fue alterada");
    }
}
