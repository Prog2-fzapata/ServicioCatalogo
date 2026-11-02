package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.web.dto;

import java.time.Instant;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncStatus;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Una base que nunca se sincronizó tiene localVersion 0 y el resto en null. lastError nunca trae datos sensibles:
// ya se guarda sin ellos
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusResponse {
    private long localVersion;
    private Instant lastSyncAt;
    private SyncType lastSyncType;
    private SyncStatus lastStatus;
    private String lastError;
}
