package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import ar.edu.um.prog2.fzapata.catalogo.LogCapture;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.logging.SyncOutcomeLogger;
import ar.edu.um.prog2.fzapata.catalogo.shared.config.CatedraSyncProperties;
import ch.qos.logback.classic.Level;

class CatalogVersionCheckSchedulerTest {

    private final CatalogService catalogService = mock(CatalogService.class);
    private final SyncOutcomeLogger syncOutcomeLogger = mock(SyncOutcomeLogger.class);

    private final CatalogVersionCheckScheduler scheduler = new CatalogVersionCheckScheduler(catalogService,
            syncOutcomeLogger, new CatedraSyncProperties(Duration.ofMinutes(5), Duration.ofSeconds(10)));

    @Test
    void registersOneFixedDelayTaskWithTheConfiguredTimes() {
        ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

        scheduler.configureTasks(registrar);

        assertThat(registrar.getFixedDelayTaskList()).singleElement().satisfies(task -> {
            assertThat(task.getIntervalDuration()).isEqualTo(Duration.ofMinutes(5));
            assertThat(task.getInitialDelayDuration()).isEqualTo(Duration.ofSeconds(10));
        });
        assertThat(registrar.getFixedRateTaskList()).isEmpty();
    }

    @Test
    void logsTheResultOfTheVersionCheck() {
        Optional<SyncResult> result = Optional.of(new SyncResult(4, 7, SyncType.INCREMENTAL));
        when(catalogService.synchronize()).thenReturn(result);

        scheduler.check();

        verify(syncOutcomeLogger).logCheck(result);
    }

    // Spring registra y sigue ante una tarea repetida que lanza, pero el aviso propio dice qué falló y que se reintenta
    @Test
    void handlesAFailureItselfInsteadOfLettingItEscape() {
        when(catalogService.synchronize()).thenThrow(new CatedraUnavailableException("sin conexión", null));

        assertThatCode(scheduler::check).doesNotThrowAnyException();
    }

    @Test
    void aFailedRoundLeavesAnErrorLineWithTheExceptionAndLogsNothingElse() {
        CatedraUnavailableException failure = new CatedraUnavailableException("sin conexión", null);
        when(catalogService.synchronize()).thenThrow(failure);

        try (LogCapture capture = LogCapture.of(CatalogVersionCheckScheduler.class)) {
            scheduler.check();

            assertThat(capture.events()).singleElement().satisfies(line -> {
                assertThat(line.getLevel()).isEqualTo(Level.ERROR);
                assertThat(line.getFormattedMessage())
                        .isEqualTo("La verificación de versión falló; se reintenta en la próxima vuelta");
                assertThat(line.getThrowableProxy().getClassName())
                        .isEqualTo(CatedraUnavailableException.class.getName());
                assertThat(LogCapture.fieldsOf(line)).isEmpty();
            });
        }
        verify(syncOutcomeLogger, never()).logCheck(any());
    }
}
