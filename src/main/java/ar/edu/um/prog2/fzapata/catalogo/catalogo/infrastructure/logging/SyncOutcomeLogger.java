package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.logging;

import java.util.Optional;

import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import lombok.extern.slf4j.Slf4j;

// Los casos de uso no loguean (A19): quien los dispara registra el resultado, y lo hace siempre igual
@Slf4j
@Component
public class SyncOutcomeLogger {

    // Qué disparó la sincronización. Un evento lleva sus ids como campos; la verificación periódica no tiene
    private record Origin(String name, Level whenNothingToApply, String eventId, Long eventVersion) {
    }

    // Un evento que no cambia nada (duplicado o ya al día) es una traza útil de la deduplicación
    public void logEvent(String eventId, long eventVersion, Optional<SyncResult> result) {
        log(new Origin("Evento de catálogo", Level.INFO, eventId, eventVersion), result);
    }

    // La verificación corre cada minuto: sin cambios va a DEBUG para no llenar el log de líneas iguales
    public void logCheck(Optional<SyncResult> result) {
        log(new Origin("Verificación de versión", Level.DEBUG, null, null), result);
    }

    private LoggingEventBuilder line(Level level, Origin origin, String message) {
        LoggingEventBuilder event = log.atLevel(level).setMessage(origin.name() + ": " + message);
        if (origin.eventId() != null) {
            event = event.addKeyValue("eventId", origin.eventId()).addKeyValue("eventVersion", origin.eventVersion());
        }
        return event;
    }

    private void log(Origin origin, Optional<SyncResult> result) {
        if (result.isEmpty()) {
            line(origin.whenNothingToApply(), origin, "nada que aplicar (ya procesado o la copia ya está al día)")
                    .log();
            return;
        }
        SyncResult applied = result.get();
        if (applied.newVersion() < applied.previousVersion()) {
            // La cátedra es la fuente de verdad: se aplicó igual, pero conviene dejar rastro
            line(Level.WARN, origin, "la cátedra retrocedió la versión del catálogo")
                    .addKeyValue("fromVersion", applied.previousVersion())
                    .addKeyValue("toVersion", applied.newVersion()).log();
        }
        LoggingEventBuilder event = line(Level.INFO, origin, "aplicado").addKeyValue("syncType", applied.type().name())
                .addKeyValue("fromVersion", applied.previousVersion()).addKeyValue("toVersion", applied.newVersion());
        if (applied.snapshotReason() != null) {
            event = event.addKeyValue("snapshotReason", applied.snapshotReason());
        }
        event.log();
    }
}
