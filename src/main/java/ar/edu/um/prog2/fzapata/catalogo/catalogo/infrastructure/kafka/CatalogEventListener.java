package ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.application.service.CatalogService;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.infrastructure.logging.SyncOutcomeLogger;
import lombok.RequiredArgsConstructor;

// Adaptador de entrada: convierte el mensaje y llama al servicio; no decide nada del negocio
@Component
@RequiredArgsConstructor
public class CatalogEventListener {

    private final CatalogService catalogService;
    private final CatalogEventMapper catalogEventMapper;
    private final SyncOutcomeLogger syncOutcomeLogger;

    // El offset se confirma recién cuando el caso de uso terminó (y su transacción hizo COMMIT). Si lanza,
    // no se confirma y el manejador de errores reintenta; Kafka reentrega y eventId evita repetir el efecto
    @KafkaListener(topics = "${catedra.contract.kafka-catalog-topic}")
    public void onCatalogUpdated(String payload, Acknowledgment acknowledgment) {
        CatalogUpdatedEvent event = catalogEventMapper.toDomainModel(payload);
        syncOutcomeLogger.log("Evento " + event.eventId() + " (versión " + event.newVersion() + ")",
                catalogService.synchronizeForEvent(event));
        acknowledgment.acknowledge();
    }
}
