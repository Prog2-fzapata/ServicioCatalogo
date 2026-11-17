package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Fila única: versión de la copia local y resultado del último intento de sincronización
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncState {

    // 0 significa base vacía: hace falta un snapshot
    private long localVersion;
    private Instant lastSyncAt;
    private SyncType lastSyncType;
    private SyncStatus lastStatus;
    private String lastError;

    // Un éxito deja la copia en esa versión y sin ningún error pendiente
    public void recordSuccess(long version, SyncType type, Instant now) {
        this.localVersion = version;
        this.lastSyncAt = now;
        this.lastSyncType = type;
        this.lastStatus = SyncStatus.OK;
        this.lastError = null;
    }

    // Un fallo no cambia localVersion: la copia quedó como estaba
    public void recordFailure(SyncType type, String error, Instant now) {
        this.lastSyncAt = now;
        this.lastSyncType = type;
        this.lastStatus = SyncStatus.ERROR;
        this.lastError = error;
    }

    // Limpia el error de una vuelta anterior. No cambia localVersion ni el tipo del último intento: la copia es la misma
    public void clearError(Instant now) {
        this.lastSyncAt = now;
        this.lastStatus = SyncStatus.OK;
        this.lastError = null;
    }
}
