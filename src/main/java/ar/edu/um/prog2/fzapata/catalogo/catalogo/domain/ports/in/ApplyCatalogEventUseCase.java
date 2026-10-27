package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogIncrement;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;

// Aplica un cambio y registra el evento que lo originó en la misma transacción.
// Vacío si el evento ya estaba procesado: no cambia nada
public interface ApplyCatalogEventUseCase {

    Optional<SyncResult> applyEvent(CatalogUpdatedEvent event, CatalogSnapshot snapshot);

    // Es la última versión de una sincronización incremental: recién acá el evento queda procesado
    Optional<SyncResult> applyEvent(CatalogUpdatedEvent event, CatalogIncrement increment);
}
