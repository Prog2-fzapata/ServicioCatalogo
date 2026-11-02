package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.scheduling;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.logging.SyncOutcomeLogger;
import ar.edu.um.prog2.fzapata.catalogo.shared.config.CatedraSyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Adaptador de entrada, como el listener de Kafka: dispara la verificación de versión poco después de arrancar y
// después cada cierto tiempo. Recupera lo que una notificación perdida dejó atrasado y, con la base vacía, trae el
// snapshot. El interruptor (activo por defecto) lo apagan los tests para que no consulte una cátedra ficticia
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "catedra.sync.check-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CatalogVersionCheckScheduler implements SchedulingConfigurer {

    private final CatalogService catalogService;
    private final SyncOutcomeLogger syncOutcomeLogger;
    private final CatedraSyncProperties properties;

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        // Espera fija entre el fin de una vuelta y el comienzo de la siguiente: dos vueltas nunca se solapan
        registrar.addFixedDelayTask(new FixedDelayTask(this::check, properties.checkInterval(),
                properties.initialDelay()));
    }

    // Un fallo (la cátedra o Redis caídos) no debe tumbar el servicio ni cortar las vueltas siguientes: se registra
    // y se reintenta en la próxima. Si el snapshot falla, el error ya quedó en sync_state
    void check() {
        try {
            syncOutcomeLogger.log("Verificación de versión", catalogService.synchronize());
        } catch (RuntimeException failure) {
            log.error("La verificación de versión falló; se reintenta en la próxima vuelta", failure);
        }
    }
}
