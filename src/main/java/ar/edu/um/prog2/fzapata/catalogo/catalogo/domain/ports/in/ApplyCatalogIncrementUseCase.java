package ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.ports.in;

import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.CatalogIncrement;
import ar.edu.um.prog2.fzapata.catalogo.catalogo.domain.model.SyncResult;

public interface ApplyCatalogIncrementUseCase {

    // Falla si la versión no es la siguiente a la local: las versiones se aplican en orden y sin saltos
    SyncResult applyIncrement(CatalogIncrement increment);
}
