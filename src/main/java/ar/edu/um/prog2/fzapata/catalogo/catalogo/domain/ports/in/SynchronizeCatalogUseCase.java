package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import java.util.Optional;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogUpdatedEvent;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;

public interface SynchronizeCatalogUseCase {

    // Falla con la excepción original; si además falló el registro del error, viaja como suprimida
    SyncResult synchronizeFull();

    // Verificación de versión (al arrancar y periódica): la misma decisión que synchronizeForEvent pero sin evento
    // que la dispare, así que no registra ningún eventId. Recupera lo que una notificación perdida dejó atrasado.
    // Vacío si la copia ya está al día. Falla igual que synchronizeFull
    Optional<SyncResult> synchronize();

    // Decide entre sincronización incremental y completa según la versión local y las que publica la cátedra, y
    // si la incremental falla cae al snapshot. Vacío si no hay nada que aplicar (evento ya procesado o copia al
    // día). Falla igual que synchronizeFull
    Optional<SyncResult> synchronizeForEvent(CatalogUpdatedEvent event);
}
