package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.logging;

import java.util.Optional;

import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import lombok.extern.slf4j.Slf4j;

// Los casos de uso no loguean (A19): quien los dispara registra el resultado, y lo hace siempre igual
@Slf4j
@Component
public class SyncOutcomeLogger {

    // origin dice qué disparó la sincronización, por ejemplo "Evento x (versión 7)"
    public void log(String origin, Optional<SyncResult> result) {
        if (result.isEmpty()) {
            log.info("{}: nada que aplicar (ya procesado o la copia ya está al día)", origin);
            return;
        }
        SyncResult applied = result.get();
        if (applied.newVersion() < applied.previousVersion()) {
            // La cátedra es la fuente de verdad: se aplicó igual, pero conviene dejar rastro
            log.warn("{}: la cátedra retrocedió de la versión {} a la {}", origin, applied.previousVersion(),
                    applied.newVersion());
        }
        log.info("{}: aplicado como {}, catálogo de la versión {} a la {}{}", origin, applied.type(),
                applied.previousVersion(), applied.newVersion(),
                applied.snapshotReason() == null ? "" : " (snapshot porque " + applied.snapshotReason() + ")");
    }
}
