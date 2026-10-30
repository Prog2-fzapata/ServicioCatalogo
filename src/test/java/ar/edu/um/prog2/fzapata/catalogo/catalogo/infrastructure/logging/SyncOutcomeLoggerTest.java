package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;

@ExtendWith(OutputCaptureExtension.class)
class SyncOutcomeLoggerTest {

    private final SyncOutcomeLogger logger = new SyncOutcomeLogger();

    @Test
    void saysThereWasNothingToApplyWhenTheResultIsEmpty(CapturedOutput output) {
        logger.log("Verificación de versión", Optional.empty());

        assertThat(output).contains("Verificación de versión: nada que aplicar");
    }

    @Test
    void reportsTheTypeAndTheVersionsOfAnIncrementalSync(CapturedOutput output) {
        logger.log("Evento e1 (versión 7)", Optional.of(new SyncResult(4, 7, SyncType.INCREMENTAL)));

        assertThat(output).contains("Evento e1 (versión 7): aplicado como INCREMENTAL, catálogo de la versión 4 a la 7")
                .doesNotContain("snapshot porque").doesNotContain("retrocedió");
    }

    @Test
    void explainsWhyASnapshotWasUsed(CapturedOutput output) {
        logger.log("Evento e1 (versión 7)",
                Optional.of(new SyncResult(0, 7, SyncType.FULL, "la base está vacía")));

        assertThat(output).contains("aplicado como FULL").contains("(snapshot porque la base está vacía)");
    }

    // La cátedra es la fuente de verdad: se aplica igual, pero queda un aviso
    @Test
    void warnsWhenTheCatedraWentBackwards(CapturedOutput output) {
        logger.log("Evento e1 (versión 5)", Optional.of(new SyncResult(9, 5, SyncType.FULL)));

        assertThat(output).contains("WARN").contains("la cátedra retrocedió de la versión 9 a la 5");
    }
}
