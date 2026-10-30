package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.exception.CatedraUnavailableException;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncType;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.logging.SyncOutcomeLogger;
import ar.edu.um.prog2.fzapata.catalogo.shared.config.CatedraSyncProperties;

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

        verify(syncOutcomeLogger).log("Verificación de versión", result);
    }

    // Spring registra y sigue ante una tarea repetida que lanza, pero el aviso propio dice qué falló y que se reintenta
    @Test
    void handlesAFailureItselfInsteadOfLettingItEscape() {
        when(catalogService.synchronize()).thenThrow(new CatedraUnavailableException("sin conexión", null));

        assertThatCode(scheduler::check).doesNotThrowAnyException();
    }
}
