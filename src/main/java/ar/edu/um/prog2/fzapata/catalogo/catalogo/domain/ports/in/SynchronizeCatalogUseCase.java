package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;

public interface SynchronizeCatalogUseCase {

    // Falla con la excepción original; si además falló el registro del error, viaja como suprimida
    SyncResult synchronizeFull();

    // Vacío si el evento ya estaba procesado o la copia local ya tiene esa versión (no se pide nada a la cátedra).
    // Falla igual que synchronizeFull
    Optional<SyncResult> synchronizeForEvent(CatalogUpdatedEvent event);
}
