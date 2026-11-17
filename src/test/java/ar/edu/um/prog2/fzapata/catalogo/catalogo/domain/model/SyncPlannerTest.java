package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

// La cátedra publica hasta la versión 9 y conserva el detalle de cambios desde la 3
class SyncPlannerTest {

    private static final SyncMetadata CATEDRA = new SyncMetadata(9, 3, Instant.parse("2026-11-17T12:00:00Z"));

    @Test
    void aCopyWithTheCurrentVersionIsUpToDate() {
        assertThat(SyncPlanner.plan(9, CATEDRA)).isEqualTo(new SyncPlan.UpToDate());
    }

    // El caso límite del orden: con la cátedra también en 0, «al día» gana a «base vacía»
    @Test
    void anEmptyCatedraAndAnEmptyCopyAreUpToDateAndNotAnEmptyDatabase() {
        assertThat(SyncPlanner.plan(0, new SyncMetadata(0, 0, Instant.EPOCH))).isEqualTo(new SyncPlan.UpToDate());
    }

    @Test
    void anEmptyDatabaseNeedsASnapshot() {
        assertThat(SyncPlanner.plan(0, CATEDRA)).isEqualTo(new SyncPlan.Snapshot("la base está vacía"));
    }

    @Test
    void anEmptyDatabaseNeedsASnapshotEvenIfTheCatedraKeepsTheWholeHistory() {
        assertThat(SyncPlanner.plan(0, new SyncMetadata(9, 0, Instant.EPOCH)))
                .isEqualTo(new SyncPlan.Snapshot("la base está vacía"));
    }

    @Test
    void aLocalVersionAheadOfTheCatedraIsABackwardsStepAndNeedsASnapshot() {
        assertThat(SyncPlanner.plan(12, CATEDRA))
                .isEqualTo(new SyncPlan.Snapshot("la cátedra retrocedió de la versión 12 a la 9"));
    }

    @Test
    void aLocalVersionOlderThanTheOldestAvailableNeedsASnapshot() {
        assertThat(SyncPlanner.plan(2, CATEDRA)).isEqualTo(new SyncPlan.Snapshot(
                "la versión local 2 es anterior a la más antigua disponible (3)"));
    }

    @Test
    void aLocalVersionExactlyTheOldestAvailableStillUsesIncrements() {
        assertThat(SyncPlanner.plan(3, CATEDRA)).isEqualTo(new SyncPlan.Incremental(3, 9));
    }

    @Test
    void aLocalVersionBetweenTheOldestAndTheCurrentUsesIncrements() {
        assertThat(SyncPlanner.plan(5, CATEDRA)).isEqualTo(new SyncPlan.Incremental(5, 9));
    }

    @Test
    void aCopyOneVersionBehindUsesASingleIncrement() {
        assertThat(SyncPlanner.plan(8, CATEDRA)).isEqualTo(new SyncPlan.Incremental(8, 9));
    }
}
