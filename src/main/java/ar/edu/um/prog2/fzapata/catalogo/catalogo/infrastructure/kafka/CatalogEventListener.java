package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.kafka;

import java.util.Optional;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Adaptador de entrada: convierte el mensaje y llama al servicio; no decide nada del negocio
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogEventListener {

    private final CatalogService catalogService;
    private final CatalogEventMapper catalogEventMapper;

    private void logOutcome(CatalogUpdatedEvent event, Optional<SyncResult> result) {
        if (result.isEmpty()) {
            log.info("Evento {} (versión {}) ignorado: ya procesado o la copia ya está al día",
                    event.eventId(), event.newVersion());
        } else if (result.get().newVersion() < result.get().previousVersion()) {
            // La cátedra es la fuente de verdad: se aplicó igual, pero conviene dejar rastro
            log.warn("La cátedra retrocedió de la versión {} a la {} (evento {})",
                    result.get().previousVersion(), result.get().newVersion(), event.eventId());
        } else {
            log.info("Evento {} aplicado: catálogo de la versión {} a la {}",
                    event.eventId(), result.get().previousVersion(), result.get().newVersion());
        }
    }

    // El offset se confirma recién cuando el caso de uso terminó (y su transacción hizo COMMIT). Si lanza,
    // no se confirma y el manejador de errores reintenta; Kafka reentrega y eventId evita repetir el efecto
    @KafkaListener(topics = "${catedra.contract.kafka-catalog-topic}")
    public void onCatalogUpdated(String payload, Acknowledgment acknowledgment) {
        CatalogUpdatedEvent event = catalogEventMapper.toDomainModel(payload);
        logOutcome(event, catalogService.synchronizeForEvent(event));
        acknowledgment.acknowledge();
    }
}
