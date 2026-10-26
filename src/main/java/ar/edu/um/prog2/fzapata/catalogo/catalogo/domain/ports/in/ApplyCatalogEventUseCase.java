package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogSnapshot;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;

public interface ApplyCatalogEventUseCase {

    // Vacío si el evento ya estaba procesado: no cambia nada
    Optional<SyncResult> applyEvent(CatalogUpdatedEvent event, CatalogSnapshot snapshot);
}
