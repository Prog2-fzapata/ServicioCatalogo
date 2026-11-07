package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ar.edu.um.prog2.fzapata.catalogo.LogCapture;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

class SyncOutcomeLoggerTest {

    private final SyncOutcomeLogger logger = new SyncOutcomeLogger();

    private LogCapture capture;

    @BeforeEach
    void captureTheLog() {
        capture = LogCapture.of(SyncOutcomeLogger.class);
    }

    @AfterEach
    void releaseTheLog() {
        capture.close();
    }

    private ILoggingEvent theOnlyLine() {
        assertThat(capture.events()).hasSize(1);
        return capture.events().getFirst();
    }

    @Test
    void anEventWithNothingToApplyIsInfoWithItsIdAndVersion() {
        logger.logEvent("e1", 7, Optional.empty());

        ILoggingEvent line = theOnlyLine();
        assertThat(line.getLevel()).isEqualTo(Level.INFO);
        assertThat(line.getFormattedMessage())
                .isEqualTo("Evento de catálogo: nada que aplicar (ya procesado o la copia ya está al día)");
        assertThat(LogCapture.fieldsOf(line)).isEqualTo(Map.of("eventId", "e1", "eventVersion", 7L));
    }

    // Corre cada pocos minutos: si no hay nada que aplicar no debe llenar el log de líneas iguales
    @Test
    void aVersionCheckWithNothingToApplyIsOnlyDebug() {
        logger.logCheck(Optional.empty());

        ILoggingEvent line = theOnlyLine();
        assertThat(line.getLevel()).isEqualTo(Level.DEBUG);
        assertThat(line.getFormattedMessage()).startsWith("Verificación de versión: nada que aplicar");
        assertThat(LogCapture.fieldsOf(line)).isEmpty();
    }

    @Test
    void anIncrementalSyncOfAnEventReportsItsTypeAndVersions() {
        logger.logEvent("e1", 7, Optional.of(new SyncResult(4, 7, SyncType.INCREMENTAL)));

        ILoggingEvent line = theOnlyLine();
        assertThat(line.getLevel()).isEqualTo(Level.INFO);
        assertThat(line.getFormattedMessage()).isEqualTo("Evento de catálogo: aplicado");
        assertThat(LogCapture.fieldsOf(line)).isEqualTo(Map.of("eventId", "e1", "eventVersion", 7L,
                "syncType", "INCREMENTAL", "fromVersion", 4L, "toVersion", 7L));
    }

    @Test
    void aVersionCheckThatAppliedSomethingLeavesNoEventFields() {
        logger.logCheck(Optional.of(new SyncResult(4, 7, SyncType.INCREMENTAL)));

        ILoggingEvent line = theOnlyLine();
        assertThat(line.getLevel()).isEqualTo(Level.INFO);
        assertThat(line.getFormattedMessage()).isEqualTo("Verificación de versión: aplicado");
        assertThat(LogCapture.fieldsOf(line))
                .isEqualTo(Map.of("syncType", "INCREMENTAL", "fromVersion", 4L, "toVersion", 7L));
    }

    @Test
    void explainsWhyASnapshotWasUsed() {
        logger.logCheck(Optional.of(new SyncResult(0, 7, SyncType.FULL, "la base está vacía")));

        assertThat(LogCapture.fieldsOf(theOnlyLine())).isEqualTo(Map.of("syncType", "FULL", "fromVersion", 0L,
                "toVersion", 7L, "snapshotReason", "la base está vacía"));
    }

    // La cátedra es la fuente de verdad: se aplica igual, pero queda un aviso antes del resultado
    @Test
    void warnsWhenTheCatedraWentBackwardsAndThenReportsTheResult() {
        logger.logEvent("e1", 5, Optional.of(new SyncResult(9, 5, SyncType.FULL)));

        assertThat(capture.events()).hasSize(2);
        ILoggingEvent warning = capture.events().getFirst();
        assertThat(warning.getLevel()).isEqualTo(Level.WARN);
        assertThat(warning.getFormattedMessage())
                .isEqualTo("Evento de catálogo: la cátedra retrocedió la versión del catálogo");
        assertThat(LogCapture.fieldsOf(warning))
                .isEqualTo(Map.of("eventId", "e1", "eventVersion", 5L, "fromVersion", 9L, "toVersion", 5L));
        assertThat(capture.events().getLast().getFormattedMessage()).isEqualTo("Evento de catálogo: aplicado");
    }

    @Test
    void aSyncThatDidNotGoBackwardsDoesNotWarn() {
        logger.logEvent("e1", 7, Optional.of(new SyncResult(7, 7, SyncType.FULL)));

        assertThat(capture.events()).extracting(ILoggingEvent::getLevel).containsExactly(Level.INFO);
    }
}
