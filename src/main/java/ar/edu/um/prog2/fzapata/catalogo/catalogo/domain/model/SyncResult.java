package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

// Quien llama decide qué registrar: si newVersion es menor que previousVersion la cátedra retrocedió.
// snapshotReason explica por qué se aplicó un snapshot y no la incremental; es null si fue incremental
public record SyncResult(long previousVersion, long newVersion, SyncType type, String snapshotReason) {

    public SyncResult(long previousVersion, long newVersion, SyncType type) {
        this(previousVersion, newVersion, type, null);
    }

    public SyncResult withSnapshotReason(String reason) {
        return new SyncResult(previousVersion, newVersion, type, reason);
    }
}
