package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

// La decisión de cómo sincronizar, sin leer Redis ni la base: solo compara versiones. Sin estado, así que no se inyecta
public final class SyncPlanner {

    private SyncPlanner() {
    }

    // El orden importa: «al día» gana a «base vacía» (0 contra 0), y una versión local mayor no se ignora, puede ser
    // un retroceso de la cátedra, que es la fuente de verdad (D60)
    public static SyncPlan plan(long localVersion, SyncMetadata metadata) {
        if (localVersion == metadata.currentVersion()) {
            return new SyncPlan.UpToDate();
        }
        if (localVersion == 0) {
            return new SyncPlan.Snapshot("la base está vacía");
        }
        if (localVersion > metadata.currentVersion()) {
            return new SyncPlan.Snapshot("la cátedra retrocedió de la versión " + localVersion + " a la "
                    + metadata.currentVersion());
        }
        if (localVersion < metadata.oldestAvailableVersion()) {
            return new SyncPlan.Snapshot("la versión local " + localVersion + " es anterior a la más antigua "
                    + "disponible (" + metadata.oldestAvailableVersion() + ")");
        }
        return new SyncPlan.Incremental(localVersion, metadata.currentVersion());
    }
}
