package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;

public interface SynchronizeCatalogUseCase {

    // Falla con la excepción original; si además falló el registro del error, viaja como suprimida
    SyncResult synchronizeFull();
}
