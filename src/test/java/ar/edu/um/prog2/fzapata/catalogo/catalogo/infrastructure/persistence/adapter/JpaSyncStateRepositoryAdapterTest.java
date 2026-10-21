package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestConstructor;

import ar.edu.um.prog2.fzapata.catalogo.TestcontainersConfiguration;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncState;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.mapper.SyncStateMapper;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.persistence.repository.JpaSyncStateRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaSyncStateRepositoryAdapter.class, SyncStateMapper.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class JpaSyncStateRepositoryAdapterTest {

    private final JpaSyncStateRepositoryAdapter adapter;
    private final JpaSyncStateRepository jpaSyncStateRepository;
    private final EntityManager entityManager;

    // Escribe en la base y vacía la memoria de Hibernate: lo que se lee después viene de la base
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findsTheInitialRowCreatedByTheMigration() {
        SyncState state = adapter.findForUpdate().orElseThrow();

        assertThat(state.getLocalVersion()).isZero();
        assertThat(state.getLastSyncAt()).isNull();
        assertThat(state.getLastSyncType()).isNull();
        assertThat(state.getLastStatus()).isNull();
        assertThat(state.getLastError()).isNull();
    }

    @Test
    void savesEveryFieldInTheSingleRow() {
        Instant at = Instant.parse("2026-10-21T15:00:00Z");
        SyncState state = adapter.findForUpdate().orElseThrow();
        state.setLocalVersion(7);
        state.setLastSyncAt(at);
        state.setLastSyncType(SyncType.FULL);
        state.setLastStatus(SyncStatus.ERROR);
        state.setLastError("CatedraUnavailableException: sin conexión");

        adapter.save(state);
        flushAndClear();

        SyncState reloaded = adapter.findForUpdate().orElseThrow();
        assertThat(reloaded.getLocalVersion()).isEqualTo(7);
        assertThat(reloaded.getLastSyncAt()).isEqualTo(at);
        assertThat(reloaded.getLastSyncType()).isEqualTo(SyncType.FULL);
        assertThat(reloaded.getLastStatus()).isEqualTo(SyncStatus.ERROR);
        assertThat(reloaded.getLastError()).isEqualTo("CatedraUnavailableException: sin conexión");
        assertThat(jpaSyncStateRepository.count()).isEqualTo(1);
    }
}
