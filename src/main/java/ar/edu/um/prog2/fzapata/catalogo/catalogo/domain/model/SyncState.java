package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Fila única: versión de la copia local y resultado del último intento de sincronización
@Getter
@Setter
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
}
