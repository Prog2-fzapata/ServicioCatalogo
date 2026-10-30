package ar.edu.um.prog2.fzapata.catalogo.catalogo.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.out.SyncStateRepository;

@ExtendWith(MockitoExtension.class)
class GetSyncStatusUseCaseImplTest {

    @Mock
    private SyncStateRepository syncStateRepository;

    @InjectMocks
    private GetSyncStatusUseCaseImpl useCase;

    @Test
    void returnsTheStateWithoutLockingTheRow() {
        SyncState state = SyncState.builder().localVersion(5).build();
        when(syncStateRepository.find()).thenReturn(Optional.of(state));

        assertThat(useCase.getSyncStatus()).containsSame(state);

        verify(syncStateRepository).find();
    }

    @Test
    void returnsEmptyWhenTheRowIsMissing() {
        when(syncStateRepository.find()).thenReturn(Optional.empty());

        assertThat(useCase.getSyncStatus()).isEmpty();
    }
}
